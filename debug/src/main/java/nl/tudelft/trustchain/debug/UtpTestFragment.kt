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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.utp4j.data.UtpPacket
import net.utp4j.data.UtpPacketUtils
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.utp.UtpCommunity
import nl.tudelft.ipv8.messaging.utp.UtpHelper
import nl.tudelft.ipv8.messaging.utp.UtpHelper.NamedResource
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import nl.tudelft.trustchain.debug.databinding.PeerComponentBinding
import java.net.DatagramPacket
import java.net.InetAddress
import java.time.Duration
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)

    private val peers: MutableList<Peer> = mutableListOf()

    private val endpoint = getUtpCommunity().endpoint.udpEndpoint

    private val viewToPeerMap: MutableMap<View, Peer> = mutableMapOf()
    private val logMap: MutableMap<Short, TextView> = ConcurrentHashMap()
    private val connectionInfoMap: MutableMap<Short, ConnectionInfo> = ConcurrentHashMap()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        getPeers()

        // Add peers to the UI
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

        // Spinner for selecting the file
        val files = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, namedFiles)
        binding.DAOSpinner.adapter = files
        binding.DAOSpinner.setSelection(0)

        // Toggle switch for selecting random data and file data
        binding.DAOToggleSwitch.setOnClickListener {
            if (binding.DAOToggleSwitch.isChecked) {
                // Use random data
                binding.DAOSpinner.isEnabled = false
                binding.editDAOText.isEnabled = true
            } else {
                // Use CSV files
                binding.DAOSpinner.isEnabled = true
                binding.editDAOText.isEnabled = false
            }
        }

        // Send data after clicking on the send button to localhost
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
            }
        }

        // Update peer status indicators
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                updatePeersStatus()
                delay(5000)
            }
        }

        // Listen for incoming packets
        val onPacket = { packet: DatagramPacket, incoming: Boolean ->
            val utpPacket = UtpPacketUtils.extractUtpPacket(packet)

             // listen for final packet being sent
             // this will tell us what the final ack of the connection will be
             if (!incoming) {
                 registerPacket(utpPacket, (utpPacket.connectionId-1).toShort())
                 if (utpPacket.windowSize == 0) {
                     println("captured last packet: ack " + utpPacket.sequenceNumber)
                     val connectionInfo = connectionInfoMap[(utpPacket.connectionId - 1).toShort()]
                     val finalAck = utpPacket.sequenceNumber.toInt()
                     connectionInfo?.finalPacket =finalAck


                     // if acknowledgement of last packet was already received, already finalize log
                     if (connectionInfo != null && connectionInfo.receivedPackets.contains((finalAck-1).toShort())) {
                         finalizeConnectionLog(utpPacket.connectionId)
                     }
                 }
             } else {

                 if (UtpPacketUtils.isSynPkt(utpPacket)) {
                     startConnectionLog((utpPacket.connectionId + 1).toShort(), packet.address, packet.port, false)
                 }
                 else if (utpPacket.ackNumber == 1.toShort()) {
                     startConnectionLog(utpPacket.connectionId, packet.address, packet.port, true)
                 }
                 else if (utpPacket.windowSize == 0) {
                     finalizeConnectionLog(utpPacket.connectionId)
                 } else if (utpPacket.sequenceNumber > 0){
                     //
                     registerPacket(utpPacket, utpPacket.connectionId)
                     logLatestPacket(utpPacket)
                 } else if (utpPacket.ackNumber > 0) {
                     logLatestPacket(utpPacket)
                     val finalPacket = connectionInfoMap[utpPacket.connectionId]!!.finalPacket
                     if (finalPacket != -1 && utpPacket.ackNumber.toInt() >= finalPacket - 1) {
                         finalizeConnectionLog(utpPacket.connectionId)
                     }
                 }
             }
        }

        endpoint?.utpIPv8Endpoint?.rawPacketListeners?.add(onPacket)
        endpoint?.utpIPv8Endpoint?.clientSocket?.rawPacketListeners?.add(onPacket)

        // update logs
        lifecycleScope.launchWhenCreated {
            while (isActive) {
                updateLogs()
                delay(500)
            }
        }
    }

    private fun updateLogs() {
        for (connectionId in connectionInfoMap.keys) {
            val connectionInfo = connectionInfoMap[connectionId]!!

            val logMessage = when (connectionInfo.status) {
                ConnectionStatus.CONNECTING -> String.format("%s: Connecting... %d", connectionInfo.peer, connectionId)
                ConnectionStatus.TRANSMITTING -> {
                    if (connectionInfo.sending) {
                        String.format(
                            "%s: sending data, received acknowledgement number #%d",
                            connectionInfo.peer,
                            connectionInfo.latestPacket
                        )
                    } else {
                        String.format(
                            "%s: receiving data, received sequence number #%d",
                            connectionInfo.peer,
                            connectionInfo.latestPacket
                        )
                    }
                }
                ConnectionStatus.DONE -> {
                    val dataTransferred = formatDataTransferredMessage(connectionInfo.dataTransferred)
                    val transferTime =
                        (connectionInfo.connectionEndTimestamp - connectionInfo.connectionStartTimestamp).div(1000.0)
                    val transferSpeed = formatTransferSpeed(connectionInfo.dataTransferred, transferTime)

                    String.format(
                        "%s: transfer completed: transferred %s in %.2f s (%s)",
                        connectionInfo.peer,
                        dataTransferred,
                        transferTime,
                        transferSpeed
                    )
                }
            }

            activity?.runOnUiThread {
                if (!logMap.containsKey(connectionId)) {
                    val logView = TextView(this.context)
                    logView.text = logMessage

                    binding.connectionLogLayout.addView(logView)
                    logMap.put(connectionId, logView)
                }

                val logView = logMap.get(connectionId)
                logView?.text = logMessage
                logView?.postInvalidate()
            }
        }
    }

    /**
     * Start a new connection log for the given connection.
     */
    private fun startConnectionLog(
        connectionId: Short,
        source: InetAddress,
        port: Int,
        sending: Boolean
    ) {
        // do not recreate log for same connection
        if (logMap.containsKey(connectionId)) {
            return
        }

        // get peer corresponding to source ip
        val peer = getPeerIdFromIp(source.toString().substring(1), port)

        // store info on connection in map
        connectionInfoMap.put(
            connectionId,
            ConnectionInfo(source, SystemClock.uptimeMillis(), sending, peer = peer)
        )
    }

    private fun isConnectionKnown(utpPacket: UtpPacket): Boolean {
        return logMap.containsKey(utpPacket.connectionId)
    }

    private fun getPeerIdFromIp(
        ip: String,
        port: Int
    ): String {
        // check if peer is self
        val myPeer = getUtpCommunity().myPeer
        val myLan = getUtpCommunity().myEstimatedLan
        if (myLan.ip == ip && myLan.port == port) {
            return myPeer.publicKey.toString().substring(0, 6)
        }

        // find peer
        val peer = peers.firstOrNull { peer -> peer.address.ip == ip && peer.address.port == port }
            ?: return "unknown"

        return peer.publicKey.toString().substring(0, 6)
    }

    private fun registerPacket(
        utpPacket: UtpPacket,
        connectionId: Short
    ) {
        synchronized(connectionInfoMap) {
            val connectionInfo = connectionInfoMap[connectionId]
            val packetNumber = maxOf(utpPacket.ackNumber, utpPacket.sequenceNumber)

            connectionInfo?.let {

                if (!it.receivedPackets.contains(packetNumber)) {
                    it.receivedPackets.add(packetNumber)
                    it.dataTransferred += utpPacket.payload.size
                }

            }
        }
    }

    private fun logLatestPacket(utpPacket: UtpPacket) {
        synchronized(connectionInfoMap) {
            if (!isConnectionKnown(utpPacket))
                return

            val connectionInfo = connectionInfoMap[utpPacket.connectionId]!!
            val latestPacket = maxOf(utpPacket.ackNumber, utpPacket.sequenceNumber)
            connectionInfo.latestPacket = maxOf(connectionInfo.latestPacket, latestPacket)

            if (connectionInfo.status == ConnectionStatus.CONNECTING) {
                connectionInfo.status = ConnectionStatus.TRANSMITTING
            }
        }
    }

    private fun finalizeConnectionLog(
        connectionId: Short
    ) {
        synchronized(connectionInfoMap) {
            // if connection is not know, do nothing
            if (!connectionInfoMap.containsKey(connectionId)) {
                return
            }

            val connectionInfo = connectionInfoMap[connectionId]!!

            if (connectionInfo.status == ConnectionStatus.DONE) {
                Log.e(LOG_TAG, "Connection $connectionId is already finished")return
            }

            connectionInfo.status = ConnectionStatus.DONE
                connectionInfo.connectionEndTimestamp = SystemClock.uptimeMillis()
        }
    }

    private fun formatDataTransferredMessage(numBytes: Int): String {
        return if (numBytes < 1_000) {
            String.format("%d B", numBytes)
        } else if (numBytes < 1_000_000) {
            return String.format("%.2f KB", numBytes.div(1_000.0))
        } else {
            return String.format("%.2f MB", numBytes.div(1_000_000.0))
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
            return String.format("%.2f KB/s", bytesPerSecond / 1_000)
        } else {
            return String.format("%.2f MB/s", bytesPerSecond / 1_000_000)
        }
    }

    /**
     * Send test data (set file or random data) to the given peer.
     */
    private fun sendTestData(peer: Peer) {
        if (binding.DAOToggleSwitch.isChecked) {
            val size = getRandomDataSize()
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

    /**
     * Send test data (set file or random data) to the given IP and port.
     * Used only for testing purposes on localhost, ignores the TransferRequestPayload handshake.
     */
    private fun sendTestData(
        ip: String,
        port: Int
    ) {
        val bytes: ByteArray
        if (binding.DAOToggleSwitch.isChecked) {
            val size = getRandomDataSize()
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

    /**
     * Get the size of the random data to be sent in bytes.
     * The size is determined by the value in the edit text field and clamped to the range [1, 50] MB.
     * If the value is invalid, the default size is 2 MiB.
     */
    private fun getRandomDataSize(): Int {
        return try {
            (binding.editDAOText.text.toString().toInt().coerceIn(1..50) * 1_000_000)
        } catch (e: NumberFormatException) {
            Log.e(LOG_TAG, "Invalid number format")
            2_048
        }
    }

    private fun getPeers() {
        Log.d(LOG_TAG, "Start peer discovery!")
        lifecycleScope.launchWhenCreated {
            val freshPeers = getUtpCommunity().getPeers()
            peers.clear()
            peers.addAll(freshPeers)
            Log.d(LOG_TAG, "Found ${peers.size} peers! ($peers)")
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

    private fun updatePeersStatus() {
        for (peer in peers) {
            val statusIndicator = findStatusIndicator(peer)
            val lastDate = getUtpCommunity().lastHeartbeat[peer.mid]
            if (lastDate == null) {
                statusIndicator.setBackgroundResource(R.drawable.indicator_gray)
            } else {
                val diff = Duration.between(Date().toInstant(), lastDate.toInstant()).abs().seconds
//                Log.d(LOG_TAG, "Time diff: $diff")
                when (diff) {
                    in 0L..30L -> statusIndicator.setBackgroundResource(R.drawable.indicator_green)
                    in 30L..60L -> statusIndicator.setBackgroundResource(R.drawable.indicator_yellow)
                    in 60L..120L -> statusIndicator.setBackgroundResource(R.drawable.indicator_orange)
                    else -> statusIndicator.setBackgroundResource(R.drawable.indicator_red)
                }
            }
        }
        // Change status indicator depending on peer status.
        // statusIndicator.setBackgroundResource(R.drawable.indicator_yellow)
    }

    private fun findStatusIndicator(peer: Peer?): View {
        // Find the status indicator in the UI for this peer
        val peerLayout =
            viewToPeerMap.entries.find { it.value == peer }?.key
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
        const val LOG_TAG = "uTP Debug"
    }

    private enum class ConnectionStatus {
        CONNECTING, TRANSMITTING, DONE
    }

    private data class ConnectionInfo(val source: InetAddress,
                                      val connectionStartTimestamp: Long,
                                      val sending: Boolean = false,
        val peer: String,
        var status: ConnectionStatus = ConnectionStatus.CONNECTING,
                                      var dataTransferred: Int = 0,
                                      var latestPacket: Short = 0,
                                      var finalPacket: Int = -1,
                                      val receivedPackets: MutableSet<Short> = ConcurrentSkipListSet(),
                                      var connectionEndTimestamp: Long = -1,
    )
}
