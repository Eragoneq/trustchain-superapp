package nl.tudelft.trustchain.debug

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import net.utp4j.data.UtpPacket
import net.utp4j.data.UtpPacketUtils
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.payload.TransferRequestPayload
import nl.tudelft.ipv8.messaging.payload.TransferRequestPayload.TransferType
import nl.tudelft.ipv8.messaging.utp.UtpCommunity
import nl.tudelft.ipv8.messaging.utp.UtpHelper
import nl.tudelft.ipv8.messaging.utp.UtpHelper.NamedResource
import nl.tudelft.ipv8.messaging.utp.UtpIPv8Endpoint.Companion.BUFFER_SIZE
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import nl.tudelft.trustchain.debug.databinding.PeerComponentBinding
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random

class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)

    private val peers: MutableList<Peer> = mutableListOf()

    private val endpoint = getUtpCommunity().endpoint.udpEndpoint

    private val viewToPeerMap: MutableMap<View, Peer> = mutableMapOf()
    private val logMap: MutableMap<Short, TextView> = HashMap()
    private val connectionInfoMap: MutableMap<Short, ConnectionInfo> = HashMap()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // create list of peers
        getPeers()

        for (peer in peers) {
            Log.d(LOG_TAG, "Adding peer " + peer.toString())
            val layoutInflater: LayoutInflater =
                this.context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val v: View = layoutInflater.inflate(R.layout.peer_component, null)

            val peerComponentBinding = PeerComponentBinding.bind(v)
            peerComponentBinding.peerIP.setText(peer.address.ip)
            peerComponentBinding.peerPublicKey.setText(peer.publicKey.toString().substring(0, 6))

            viewToPeerMap[v] = peer

            binding.peerListLayout.addView(
                v,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        // Data selection
        // Named resources
        val namedFiles =
            listOf(
                NamedResource("votes3.csv", R.raw.votes3),
                NamedResource("votes13.csv", R.raw.votes13),
            )

        binding.DAOToggleSwitch.setOnClickListener {
            if (binding.DAOToggleSwitch.isChecked) {
                // Use random data
                binding.DAOSpinner.isEnabled = false
                binding.editDAOText.isEnabled = true
            } else {
                // Use CSV files
                binding.DAOSpinner.isEnabled = true
                binding.editDAOText.isEnabled = false

                // Hardcoded files
                val files =
                    ArrayAdapter(it.context, android.R.layout.simple_spinner_item, namedFiles)
                binding.DAOSpinner.adapter = files
                binding.DAOSpinner.setSelection(0)
            }
        }

        binding.sendTestPacket.setOnClickListener {
            val myWan = getUtpCommunity().myEstimatedLan
            lifecycleScope.launchWhenCreated {
                sendTestData(myWan.ip, myWan.port)
            }
        }

        // Send data after clicking on peer component.
        for (peerView in binding.peerListLayout.children.iterator()) {
            peerView.setOnClickListener {
                val peer = viewToPeerMap[peerView] ?: error("Peer not found for view")
                val address = peer.address.toString()
                lifecycleScope.launchWhenCreated {
                    sendTestData(peer)
                }
                Log.d(LOG_TAG, "sending data to peer $address")
                updatePeerStatus(peer)
            }
        }

        endpoint?.utpIPv8Endpoint?.rawPacketListeners?.add { packet ->
            val utpPacket = UtpPacketUtils.extractUtpPacket(packet)
            if (UtpPacketUtils.isSynPkt(utpPacket)) {
                startConnectionLog(utpPacket.connectionId, packet.address)
            } else if (utpPacket.windowSize == 0) {
                finalizeConnectionLog(utpPacket.connectionId, packet.address)
            } else {
                updateConnectionLog(utpPacket, packet.address)
            }
        }
    }

    private fun startConnectionLog(
        connectionId: Short,
        source: InetAddress
    ) {
        // do not recreate log for same connection
        if (logMap.containsKey(connectionId)) {
            return
        }

        // store info on connection in map
        connectionInfoMap.put(
            (connectionId + 1).toShort(),
            ConnectionInfo(source, SystemClock.uptimeMillis(), 0)
        )

        // create new log section in fragment
        activity?.runOnUiThread {
            val logView = TextView(this.context)
            logView.setText(String.format("%s: Connected", source))

            binding.connectionLogLayout.addView(logView)

            synchronized(logMap) {
                logMap.put((connectionId + 1).toShort(), logView)
            }
        }
    }

    private fun updateConnectionLog(
        utpPacket: UtpPacket,
        source: InetAddress
    ) {
        // if connection is not know, do nothing
        val connectionId = utpPacket.connectionId

        if (!logMap.containsKey(connectionId)) {
            return
        }

        // update ConnectionInfo
        // TODO: retransmitted packets currently count towards data transferred, but shouldn't
        connectionInfoMap[connectionId]!!.dataTransferred += utpPacket.payload.size

        // display current ack number
        // TODO: too much updating of UI causes frame drop, change to periodic update from render thread
        // temporary solution: do not display every
        if (utpPacket.sequenceNumber % 50 == 0) {
            activity?.runOnUiThread {
                val logView = logMap.get(connectionId)

                logView?.setText(
                    String.format(
                        "%s: receiving data, sequence number #%d",
                        source,
                        utpPacket.sequenceNumber
                    )
                )
                logView?.postInvalidate()
            }
        }
    }

    private fun finalizeConnectionLog(
        connectionId: Short,
        source: InetAddress
    ) {
        // if connection is not know, do nothing
        if (!logMap.containsKey(connectionId)) {
            return
        }

        val connectionInfo = connectionInfoMap[connectionId]!!

        // display current ack number
        activity?.runOnUiThread {
            val logView = logMap.get(connectionId)

            val dataTransferred = formatDataTransferredMessage(connectionInfo.dataTransferred)
            val transferTime =
                (SystemClock.uptimeMillis() - connectionInfo.connectionStartTimestamp).div(1000.0)
            val transferSpeed = formatTransferSpeed(connectionInfo.dataTransferred, transferTime)

            logView?.setText(
                String.format(
                    "%s: transfer completed: received %s in %.2f s (%s)",
                    source,
                    dataTransferred,
                    transferTime,
                    transferSpeed
                )
            )
            logView?.postInvalidate()
        }
    }

    private fun formatDataTransferredMessage(numBytes: Int): String {
        if (numBytes < 1_000) {
            return String.format("%d B", numBytes)
        } else if (numBytes < 1_000_000) {
            return String.format("%.2f KiB", numBytes.div(1_000.0))
        } else {
            return String.format("%.2f MiB", numBytes.div(1_000_000.0))
        }
    }

    private fun formatTransferSpeed(
        numBytes: Int,
        time: Double
    ): String {
        val bytesPerSecond = numBytes.div(time)

        return if (bytesPerSecond < 1_000) {
            String.format("%.2f B/s", bytesPerSecond)
        } else if (bytesPerSecond < 1_000_000) {
            String.format("%.2f KiB/s", bytesPerSecond / 1_000)
        } else {
            String.format("%.2f MiB/s", bytesPerSecond / 1_000_000)
        }
    }

    private fun sendTestData(peer: Peer) {
        if (binding.DAOToggleSwitch.isChecked) {
            val size = binding.editDAOText.text.toString().toInt()
            getUtpCommunity().utpHelper.sendRandomData(peer, size)
            return
        }
        val item = binding.DAOSpinner.selectedItem as NamedResource?
        if (item == null) {
            Log.e(LOG_TAG, "No file selected")
            return
        }
        val csv = resources.openRawResource(item.id)
        val bytes = csv.readBytes()
        csv.close()
        val item1 = item.copy(size = bytes.size)
        getUtpCommunity().utpHelper.sendFileData(peer, item1, bytes)
    }

    private fun sendTestData(
        ip: String,
        port: Int
    ) {
        val bytes: ByteArray
        if (binding.DAOToggleSwitch.isChecked) {
            val size = binding.editDAOText.text.toString().toInt()
            bytes = UtpHelper.generateRandomDataBuffer(size)
        } else {
            val item = binding.DAOSpinner.selectedItem as NamedResource?
            if (item == null) {
                Log.e(LOG_TAG, "No file selected")
                return
            }
            val csv = resources.openRawResource(item.id)
            bytes = csv.readBytes()
            csv.close()
        }
        endpoint?.sendUtp(IPv4Address(ip, port), bytes)
    }

    private fun getPeers() {
        Log.d("uTP Client", "Start peer discovery!")
        lifecycleScope.launchWhenCreated {
            val freshPeers = getUtpCommunity().getPeers()
            peers.clear()
            peers.addAll(freshPeers)
            Log.d("uTP Client", "Found ${peers.size} peers! ($peers)")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_utp_test, container, false)
    }

    private fun updatePeerStatus(peer: Peer?) {
        val statusIndicator = findStatusIndicator(peer)
        // Change status indicator depending on peer status.
        // statusIndicator.setBackgroundResource(R.drawable.indicator_yellow)
    }

    private fun findStatusIndicator(peer: Peer?): View {
        // Find the status indicator in the UI for this peer
        val peerLayout = viewToPeerMap.entries.find { it.value == peer }?.key
            ?: error("Layout for peer $peer not found")
        val statusIndicator =
            peerLayout.findViewById<View>(
                R.id.peerStatusIndicator
            ) ?: error("Status indicator in layout for peer $peer not found")
        return statusIndicator
    }

    private fun getUtpCommunity(): UtpCommunity {
        return getIpv8().getOverlay()
            ?: throw IllegalStateException("UtpCommunity is not configured")
    }

    companion object {
        const val MIN_PORT = 1024
        const val LOG_TAG = "uTP Debug"
    }

    private data class ConnectionInfo(
        val source: InetAddress,
        val connectionStartTimestamp: Long,
        var dataTransferred: Int
    )

}
