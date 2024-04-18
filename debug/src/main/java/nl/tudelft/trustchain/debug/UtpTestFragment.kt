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

        val files = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, namedFiles)
        binding.DAOSpinner.adapter = files
        binding.DAOSpinner.setSelection(0)

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

         val onPacket = { packet: DatagramPacket, outgoing: Boolean ->
             val utpPacket = UtpPacketUtils.extractUtpPacket(packet)

             // listen for final packet being sent
             // this will tell us what the final ack of the connection will be
             if (!outgoing) {
                 registerPacket(utpPacket, (utpPacket.connectionId-1).toShort())
                 if (utpPacket.windowSize == 0) {
                     connectionInfoMap[(utpPacket.connectionId - 1).toShort()]?.finalPacket =
                         utpPacket.sequenceNumber.toInt()
                 }
             } else {

                 if (UtpPacketUtils.isSynPkt(utpPacket)) {
                     startConnectionLog((utpPacket.connectionId + 1).toShort(), packet.address, packet.port)
                 }
                 else if (utpPacket.ackNumber == 1.toShort()) {
                     startConnectionLog((utpPacket.connectionId).toShort(), packet.address, packet.port)
                 }
                 else if (utpPacket.windowSize == 0) {
                     finalizeConnectionLog(utpPacket.connectionId, packet.address)
                 } else if (utpPacket.sequenceNumber > 0){
                     //
                     logDataPacket(utpPacket, packet.address)
                 } else if (utpPacket.ackNumber > 0) {
                     logAckPacket(utpPacket, packet.address)

                     if (utpPacket.ackNumber.toInt() == connectionInfoMap[utpPacket.connectionId]!!.finalPacket) {
                         finalizeConnectionLog(utpPacket.connectionId, packet.address)
                     }
                 }
             }
        }

        endpoint?.utpIPv8Endpoint?.rawPacketListeners?.add(onPacket)
        endpoint?.utpIPv8Endpoint?.clientSocket?.rawPacketListeners?.add(onPacket)

        // update logs
        lifecycleScope.launchWhenCreated {
            while (isActive) {
                for (connectionId in connectionInfoMap.keys) {
                    val connectionInfo= connectionInfoMap[connectionId]

                    val logMessage = connectionInfo?.logMessage
                    activity?.runOnUiThread {
                        val logView = logMap.get(connectionId)
                        logView?.text = logMessage
                        logView?.postInvalidate()
                    }
                }
                delay(500)
            }
        }
    }

    private fun startConnectionLog(
        connectionId: Short,
        source: InetAddress,
        port: Int
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
            ConnectionInfo(source, SystemClock.uptimeMillis(), 0, peer = peer)
        )

        val logMessage = String.format("%s: Connecting... %d", peer, connectionId)
        // create new log section in fragment
        activity?.runOnUiThread {
            synchronized(logMap) {
                if (!logMap.containsKey(connectionId)) {
                    val logView = TextView(this.context)
                    logView.text = logMessage

                    binding.connectionLogLayout.addView(logView)
                    logMap.put(connectionId, logView)
                }
            }

        }
    }

    private fun isConnectionKnown(utpPacket: UtpPacket): Boolean {
        return logMap.containsKey(utpPacket.connectionId)
    }

    private fun getPeerIdFromIp(ip: String, port: Int): String {
        // check if peer is self
        val myPeer = getUtpCommunity().myPeer
        val myLan = getUtpCommunity().myEstimatedLan
        if (myLan.ip == ip && myLan.port == port) {
            return myPeer.publicKey.toString().substring(0, 6)
        }

        // find peer
        val peer = peers.filter { peer -> peer.address.ip == ip}.firstOrNull()

        if (peer == null) {
            return "unknown"
        }

        return peer.publicKey.toString().substring(0, 6)
    }

    private fun registerPacket(utpPacket: UtpPacket, connectionId: Short) {
        var connectionInfo = connectionInfoMap[connectionId]
        val packetNumber = maxOf(utpPacket.ackNumber, utpPacket.sequenceNumber)

        connectionInfo?.let {
            if (!it.receivedPackets.contains(packetNumber)) {
                it.receivedPackets.add(packetNumber)
                it.dataTransferred += utpPacket.payload.size
            }
        }

        // TODO: do not count retransmits towards total
    }

    private fun logAckPacket(utpPacket: UtpPacket, address: InetAddress) {
        if (!isConnectionKnown(utpPacket))
            return

        val connectionInfo = connectionInfoMap[utpPacket.connectionId]

        val logMessage = String.format(
            "%s: sending data, received acknowledgement number #%d",
            connectionInfo?.peer,
            utpPacket.ackNumber
        )

        updateConnectionLog(utpPacket.connectionId, logMessage)
    }

    private fun logDataPacket(utpPacket: UtpPacket, address: InetAddress) {
        if (!isConnectionKnown(utpPacket)) {
            println("connection " + utpPacket.connectionId + " is unknown, only have " + connectionInfoMap.keys)
            return
        }

        registerPacket(utpPacket, utpPacket.connectionId)

        val connectionInfo = connectionInfoMap[utpPacket.connectionId]

        val logMessage = String.format(
            "%s: receiving data, received sequence number #%d",
            connectionInfo?.peer,
            utpPacket.sequenceNumber
        )

        updateConnectionLog(utpPacket.connectionId, logMessage)
    }

    private fun updateConnectionLog(
        connectionId: Short,
        logMessage: String
    ) {
        val connectionInfo = connectionInfoMap.get(connectionId)
        connectionInfo?.logMessage = logMessage
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

        if (connectionInfo.finished) {
            return
        }
        connectionInfo.finished = true

        val dataTransferred = formatDataTransferredMessage(connectionInfo.dataTransferred)
        val transferTime =
            (SystemClock.uptimeMillis() - connectionInfo.connectionStartTimestamp).div(1000.0)
        val transferSpeed = formatTransferSpeed(connectionInfo.dataTransferred, transferTime)

        val logMessage = String.format(
            "%s: transfer completed: transferred %s in %.2f s (%s)",
            connectionInfo.peer,
            dataTransferred,
            transferTime,
            transferSpeed
        )

        updateConnectionLog(connectionId, logMessage)
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
        const val LOG_TAG = "uTP Debug"
    }

    private data class ConnectionInfo(val source: InetAddress,
                                      val connectionStartTimestamp: Long,
                                      var dataTransferred: Int,
                                      val peer: String,
                                      var logMessage: String = "",
                                      var finalPacket: Int = -1,
                                      var finished: Boolean = false,
                                      val receivedPackets: MutableSet<Short> = ConcurrentSkipListSet()
    )

}
