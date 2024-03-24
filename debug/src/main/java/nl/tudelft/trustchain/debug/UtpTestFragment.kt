package nl.tudelft.trustchain.debug

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.utp.UtpEndpoint
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random

class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)

    private val endpoint: UtpEndpoint? = getDemoCommunity().endpoint.utpEndpoint

    private val peers: MutableList<Peer> = mutableListOf()

    private var ipv8Mode: Boolean = true
    private var csvFileMode: Boolean = true

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Placeholder data
        binding.editIPforUTP.text = Editable.Factory.getInstance().newEditable("145.94.193.11:13377")

        // Set up spinners
        // Named resources
        val namedFiles = listOf(NamedResource("votes3", R.raw.votes3), NamedResource("votes13", R.raw.votes13))
        getPeers()
        val files = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, namedFiles)
        binding.DAOSpinner.adapter = files

        // IP Selection
        binding.IPvToggleSwitch.setOnClickListener {
            if (binding.IPvToggleSwitch.isChecked) {
                // IPv8 peers
                binding.IPvSpinner.isEnabled = true
                binding.editIPforUTP.isEnabled = false
                ipv8Mode = true
                getPeers()

            } else {
                // Usual IPv4 address
                binding.IPvSpinner.isEnabled = false
                binding.editIPforUTP.isEnabled = true
                ipv8Mode = false
            }
        }

        // Data selection
        binding.DAOToggleSwitch.setOnClickListener {
            if (binding.DAOToggleSwitch.isChecked) {
                // Use CSV files
                binding.DAOSpinner.isEnabled = true
                binding.editDAOText.isEnabled = false
                csvFileMode = true
//                binding.DAOSpinner.setSelection(0)
            } else {
                // Use random data
                binding.DAOSpinner.isEnabled = false
                binding.editDAOText.isEnabled = true
                csvFileMode = false
            }
        }

        binding.sendTestPacket.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                val data = if (csvFileMode) {
                    val selectedFile = binding.DAOSpinner.selectedItem as NamedResource
                    resources.openRawResource(selectedFile.id).readBytes()
                } else {
                    if (binding.editDAOText.text.isEmpty()) {
                        Log.d("uTP Client", "No data size specified!")
                        return@launchWhenCreated
                    }
                    generateRandomDataBuffer(binding.editDAOText.text.toString().toInt()).array()
                }

                if (ipv8Mode) {
                    if (binding.IPvSpinner.selectedItem is Peer) {
                        val peer = binding.IPvSpinner.selectedItem as Peer
                        sendTestData(peer, data)
                    } else {
                        Log.d("uTP Client", "No peer selected!")
                    }
                } else {
                    val address = binding.editIPforUTP.text.toString().split(":")
                    if (address.size == 2) {
                        val ip = address[0]
                        val port = address[1].toIntOrNull() ?: MIN_PORT
                        sendTestData(ip, port, data)
                    }
                }
            }
        }

        // Fetch data listener every second
        lifecycleScope.launchWhenCreated {
            while (isActive) {
                fetchData()
                delay(5000)
            }
        }
    }

    /**
     * Fetch data from the endpoint
     */
    private fun fetchData() {
        val data: String

        if (endpoint?.listener!!.queue.size > 0) {
            data = String(endpoint.listener.queue.removeFirst())
            Log.d("uTP Client", "Received data!")
        } else {
            data = "No data received!"
            Log.d("uTP Client", "No data received!")
        }

        view?.post {
//            val time = (endTime - startTime) / 1000
//            val speed = Formatter.formatFileSize(requireView().context, (BUFFER_SIZE / time))
            binding.logUTP.text = data.takeLast(2000) + "\n\n" + endpoint.lastTime.toString()
        }
    }

    /**
     * Send data to a specific peer
     */
    private suspend fun sendTestData(peer: Peer, data: ByteArray) {
        val demoCommunity = getDemoCommunity()
        demoCommunity.sendTransferRequest(peer, 13377, data.size)
        Log.d("uTP Client", "Sending data to $peer")
        // Await for the puncture to be successful
        while (true) {
            if (demoCommunity.puncturedUtpPort[peer] != null) {
                Log.d("uTP Client", "Puncture successful!")
                Log.d("uTP Client", "Sending data to $peer on port ${demoCommunity.puncturedUtpPort[peer]!!.port}")
                demoCommunity.endpoint.utpEndpoint?.send(peer, demoCommunity.puncturedUtpPort[peer]!!.port, data)
                break
            } else {
                Log.d("uTP Client", "Waiting for puncture to be successful!")
                delay(1000)
            }
        }
    }

    /**
     * Send data to a specific IP and port
     * We use this method when we are not connected to the IPv8 network and assume that the
     * IP and port are reachable.
     */
    private fun sendTestData(
        ip: String,
        port: Int,
        data: ByteArray
    ) {
        Log.d("uTP Client", "Sending data to $ip:$port")
        getDemoCommunity().endpoint.utpEndpoint?.send(IPv4Address(ip, port), data)
    }

    /**
     * Get peers from the IPv8 Demo Community
     */
    private fun getPeers() {
        Log.d("uTP Client", "Start peer discovery!")
        lifecycleScope.launchWhenCreated {
            val freshPeers = getDemoCommunity().getPeers()
            peers.clear()
            peers.addAll(freshPeers)
            Log.d("uTP Client", "Found ${peers.size} peers! ($peers)")

            binding.IPvSpinner.adapter =
                ArrayAdapter(
                    requireView().context,
                    android.R.layout.simple_spinner_item,
                    peers
                )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_utp_test, container, false)
    }

    private fun generateRandomDataBuffer(size: Int): ByteBuffer {
        Log.d("uTP Client", "Start preparing buffer!")
        val rngByteArray = ByteArray(size + 32)
        Random.nextBytes(rngByteArray, 0, size)
        Log.d("uTP Client", "Fill random bytes!")
        // Create hash to check correctness
        Log.d("uTP Client", "Create hash!")
        val buffer = ByteBuffer.wrap(rngByteArray)
        // Create hash to check correctness
        Log.d("uTP Client", "Create hash!")
        val hash = MessageDigest.getInstance("SHA-256").digest(rngByteArray)
        buffer.position(size)
        buffer.put(hash)
        Log.d("uTP Client", "Generated random data with hash $hash")
        return buffer
    }

    companion object {
        const val MIN_PORT = 1024

        /**
         * A simple named resource class for the spinner
         */
        data class NamedResource(val name: String, val id: Int) {
            override fun toString(): String = name
        }
    }
}
