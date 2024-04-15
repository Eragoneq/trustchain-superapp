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
import net.utp4j.data.UtpPacket
import net.utp4j.data.UtpPacketUtils
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.utp.UtpCommunity
import nl.tudelft.ipv8.messaging.utp.UtpIPv8Endpoint.Companion.BUFFER_SIZE
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import nl.tudelft.trustchain.debug.databinding.PeerComponentBinding
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random

class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)

    private val peers: MutableList<Peer> = mutableListOf()

    private val endpoint = getUtpCommunity().endpoint.udpEndpoint

    private var ipv8Mode: Boolean = false
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
            val layoutInflater: LayoutInflater = this.context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
                object {
                    val name: String = "votes3.csv"
                    val id: Int = R.raw.votes3

                    override fun toString(): String = name
                },
                object {
                    val name: String = "votes13.csv"
                    val id: Int = R.raw.votes3

                    override fun toString(): String = name
                }
            )

        binding.DAOToggleSwitch.setOnClickListener {
            if (binding.DAOToggleSwitch.isChecked) {
                // Use CSV files
                binding.DAOSpinner.isEnabled = true
                binding.editDAOText.isEnabled = false

                // Hardcoded files
                val files = ArrayAdapter(it.context, android.R.layout.simple_spinner_item, namedFiles)
                binding.DAOSpinner.adapter = files
                binding.DAOSpinner.setSelection(0)
            } else {
                // Use random data
                binding.DAOSpinner.isEnabled = false
                binding.editDAOText.isEnabled = true
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
                     println("sending final pkt seq " + utpPacket.sequenceNumber + " of connection " + (utpPacket.connectionId.toInt() - 1))
                     connectionInfoMap[(utpPacket.connectionId - 1).toShort()]?.finalPacket =
                         utpPacket.sequenceNumber.toInt()
                 }
             } else {
                 println("---- seq, ack, id: " + utpPacket.sequenceNumber + " " + utpPacket.ackNumber + " " + utpPacket.connectionId)

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
                         println("tried to fin log")
                         finalizeConnectionLog(utpPacket.connectionId, packet.address)
                     }
                 }
             }
        }

        endpoint?.utpIPv8Endpoint?.rawPacketListeners?.add(onPacket)
        endpoint?.utpIPv8Endpoint?.clientSocket?.rawPacketListeners?.add(onPacket)
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
        println(source.toString().substring(1))
        val peer = getPeerIdFromIp(source.toString().substring(1), port)
        println(peer)

        // store info on connection in map
        connectionInfoMap.put(
            connectionId,
            ConnectionInfo(source, SystemClock.uptimeMillis(), 0, peer = peer)
        )

        val logMessage = String.format("%s: Connecting... %d", peer, connectionId)

        // create new log section in fragment
        activity?.runOnUiThread {
            val logView = TextView(this.context)
            logView.text = logMessage

            binding.connectionLogLayout.addView(logView)

            synchronized(logMap) {
                logMap.put(connectionId, logView)
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
        println(myLan.ip + " " + myLan.port)
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
        val connectionInfo = connectionInfoMap[connectionId]

        connectionInfo?.let {
            it.dataTransferred += utpPacket.payload.size
        }

        // TODO: do not count retransmits towards total
    }

    private fun logAckPacket(utpPacket: UtpPacket, address: InetAddress) {
        if (!isConnectionKnown(utpPacket))
            return

        // only display every 50th ack
        // TODO: too much updating of UI causes frame drop, change to periodic update from render thread
//        if (utpPacket.ackNumber % 50 != 0)
//            return
        val connectionInfo = connectionInfoMap[utpPacket.connectionId]

        val logMessage = String.format(
            "%s: receiving data, received sequence number #%d",
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

        // only display every 50th ack
        // TODO: too much updating of UI causes frame drop, change to periodic update from render thread
        if (utpPacket.sequenceNumber % 50 != 0)
            return

        val connectionInfo = connectionInfoMap[utpPacket.connectionId]

        val logMessage = String.format(
            "%s: sending data, received ack number #%d",
            connectionInfo?.peer,
            utpPacket.sequenceNumber
        )

        updateConnectionLog(utpPacket.connectionId, logMessage)
    }

    private fun updateConnectionLog(
        connectionId: Short,
        logMessage: String
    ) {
        activity?.runOnUiThread {
            val logView = logMap.get(connectionId)
            logView?.text = logMessage
            logView?.postInvalidate()
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
        val dataTransferred = formatDataTransferredMessage(connectionInfo.dataTransferred)
        val transferTime =
            (SystemClock.uptimeMillis() - connectionInfo.connectionStartTimestamp).div(1000.0)
        val transferSpeed = formatTransferSpeed(connectionInfo.dataTransferred, transferTime)

        val logMessage = String.format(
            "%s: transfer completed: received %s in %.2f s (%s)",
            connectionInfo.peer,
            dataTransferred,
            transferTime,
            transferSpeed
        )

        updateConnectionLog(connectionId, logMessage)
    }

    private fun formatDataTransferredMessage(numBytes: Int): String {
        if (numBytes < 1_000) {
            return String.format("%d B", numBytes)
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

        if (bytesPerSecond < 1_000) {
            return String.format("%.2f B/s", bytesPerSecond)
        } else if (bytesPerSecond < 1_000_000) {
            return String.format("%.2f KB/s", bytesPerSecond / 1_000)
        } else {
            return String.format("%.2f MB/s", bytesPerSecond / 1_000_000)
        }
    }

    private fun sendTestData(peer: Peer) {
        sendTestData(peer.address.ip, peer.address.port)
    }

    private fun sendTestData(
        ip: String,
        port: Int
    ) {
        val csv3 = resources.openRawResource(R.raw.votes3)
        val csv13 = resources.openRawResource(R.raw.votes13)

//            // 100 MB of random bytes + hash
//            val buffer = generateRandomDataBuffer()
//            // Send CSV file
//            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//            buffer.put(csv3.readBytes())
        Log.d("uTP Client", "Sending data to $ip:$port")
        endpoint?.sendUtp(IPv4Address(ip, port), csv3.readBytes())
        csv3.close()
        csv13.close()
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
        val peerLayout = viewToPeerMap.entries.find { it.value == peer }?.key ?: error("Layout for peer $peer not found")
        val statusIndicator =
            peerLayout.findViewById<View>(
                R.id.peerStatusIndicator
            ) ?: error("Status indicator in layout for peer $peer not found")
        return statusIndicator
    }

    private fun generateRandomDataBuffer(): ByteBuffer {
        Log.d("uTP Client", "Start preparing buffer!")
        val rngByteArray = ByteArray(BUFFER_SIZE + 32)
        Random.nextBytes(rngByteArray, 0, BUFFER_SIZE)
        Log.d("uTP Client", "Fill random bytes!")
        // Create hash to check correctness
        Log.d("uTP Client", "Create hash!")
        val buffer = ByteBuffer.wrap(rngByteArray)
        // Create hash to check correctness
        Log.d("uTP Client", "Create hash!")
        val hash = MessageDigest.getInstance("SHA-256").digest(rngByteArray)
        buffer.position(BUFFER_SIZE)
        buffer.put(hash)
        Log.d("uTP Client", "Generated random data with hash $hash")
        return buffer
    }

    private fun getUtpCommunity(): UtpCommunity {
        return getIpv8().getOverlay() ?: throw IllegalStateException("UtpCommunity is not configured")
    }

    companion object {
        const val MIN_PORT = 1024
        const val LOG_TAG = "uTP Debug"
    }

    private data class ConnectionInfo(val source: InetAddress, val connectionStartTimestamp: Long, var dataTransferred: Int, val peer: String, var finalPacket: Int = -1)
}
