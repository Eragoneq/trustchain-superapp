package nl.tudelft.trustchain.common

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.messaging.Address
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.messaging.payload.TransferRequestPayload
import nl.tudelft.ipv8.messaging.payload.PuncturePayload
import nl.tudelft.ipv8.messaging.payload.TransferRequestPayload.TransferStatus.ACCEPT
import nl.tudelft.ipv8.messaging.payload.TransferRequestPayload.TransferStatus.REQUEST
import nl.tudelft.ipv8.messaging.utp.UtpEndpoint.Companion.BUFFER_SIZE
import java.net.DatagramPacket
import java.net.InetAddress
import java.util.Date

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    val punctureChannel = MutableSharedFlow<Pair<Address, PuncturePayload>>(0, 10000)

    val puncturedUtpPort: MutableMap<Peer, TransferRequestPayload?> = mutableMapOf()

    // Retrieve the trustchain community
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    override fun onIntroductionResponse(
        peer: Peer,
        payload: IntroductionResponsePayload
    ) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
    }

    object MessageId {
        const val PUNCTURE_TEST = 251
        const val TRANSFER_REQUEST = 252
        const val TRANSFER_REQUEST_RESPONSE = 253
    }

    fun sendPuncture(
        address: IPv4Address,
        id: Int
    ) {
        val payload = PuncturePayload(myEstimatedLan, myEstimatedWan, id)
        val packet = serializePacket(MessageId.PUNCTURE_TEST, payload, sign = false)
        endpoint.send(address, packet)
    }

    // RECEIVE MESSAGE
    init {
        messageHandlers[MessageId.PUNCTURE_TEST] = ::onPunctureTest
        messageHandlers[MessageId.TRANSFER_REQUEST] = ::onTransferRequest
        messageHandlers[MessageId.TRANSFER_REQUEST_RESPONSE] = ::onTransferRequestResponse
    }

    private fun onPunctureTest(packet: Packet) {
        val payload = packet.getPayload(PuncturePayload.Deserializer)
        punctureChannel.tryEmit(Pair(packet.source, payload))
    }

    // Client / Sender
    fun sendTransferRequest(
        peer: Peer,
        portToOpen: Int = 13377,
        dataSize: Int
    ) {
        val address = peer.address
        Log.d("uTP Client", "Sending transfer request to $address")
        val payload = TransferRequestPayload(portToOpen, REQUEST, dataSize)
        val packet = serializePacket(MessageId.TRANSFER_REQUEST, payload, sign = false)
        val datagramPacket = DatagramPacket(packet, packet.size, address.toSocketAddress())
        // Send puncture packet
        endpoint.utpEndpoint?.sendRawClientData(datagramPacket)
        endpoint.send(address, packet)
        puncturedUtpPort[peer] = null
    }


    // Server / Receiver
    private fun onTransferRequest(packet: Packet) {
        Log.d("uTP Server", "Received transfer request from ${packet.source}")
        val payload = packet
            .getPayload(TransferRequestPayload.Deserializer).let {
                TransferRequestPayload(
                    status = ACCEPT, // Accept the transfer request by default
                    port = endpoint.utpEndpoint?.port ?: 13377,
                    dataSize = if (it.dataSize > BUFFER_SIZE) BUFFER_SIZE else it.dataSize
                )
            }
        if (packet.source is IPv4Address) {
            // Transfer Request Response
            sendData(
                serializePacket(MessageId.TRANSFER_REQUEST_RESPONSE, payload, sign = false),
                (packet.source as IPv4Address).ip,
                (packet.source as IPv4Address).port,
            )
        }
    }

    /**
     * Respond to client with packet data
     */
    private fun sendData(data: ByteArray, clientIp: String, clientPort: Int) {
        Log.d("uTP Server", "Sending puncture data to $clientIp:$clientPort")
        val address = InetAddress.getByName(clientIp)
        val dgPacket = DatagramPacket(data, data.size, address, clientPort)
        Log.d("uTP Server", "Sending puncture to $address:$clientPort")
        endpoint.utpEndpoint?.sendServerData(dgPacket)

        val peer = getPeers().find {
            it.address.ip == clientIp
        }
        if (peer != null) {
            Log.d("uTP Server", "Sending response to IPv8 port ${peer.address}")
            endpoint.utpEndpoint?.sendServerData(DatagramPacket(data, data.size, InetAddress.getByName(peer.address.ip), peer.address.port))
        } else Log.d("uTP Server", "No client peer found for response!")
    }

    private fun onTransferRequestResponse(packet: Packet) {
        // Get peer with a given address
        val peer = getPeers().find {
            if (packet.source is IPv4Address) it.address.ip == (packet.source as IPv4Address).ip
            else false
        }
        val payload = packet.getPayload(TransferRequestPayload.Deserializer).let { payload ->
            TransferRequestPayload(
                port = if (packet.source is IPv4Address) (packet.source as IPv4Address).port else payload.port,
                status = payload.status,
                dataSize = payload.dataSize
            )
        }

        if (peer != null) {
            puncturedUtpPort[peer] = payload
            Log.d("uTP Client", "Received transfer request response from ${peer.address} port ${packet.source}")
        } else {
            Log.e("uTP Client", "Peer not found for ${packet.source}!")
        }
    }
}
