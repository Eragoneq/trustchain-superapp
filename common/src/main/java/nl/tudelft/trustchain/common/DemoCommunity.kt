package nl.tudelft.trustchain.common

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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Date

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    val punctureChannel = MutableSharedFlow<Pair<Address, PuncturePayload>>(0, 10000)

    var serverWanPort: Int? = null
    var senderDataSize: Int? = null
    var receivedDataSize: Int? = null

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
        address: IPv4Address,
        portToOpen: Int,
        dataSize: Int
    ) {
        val payload = TransferRequestPayload(portToOpen, dataSize)
        val packet = serializePacket(MessageId.TRANSFER_REQUEST, payload, sign = false)
        endpoint.send(address, packet)
    }


    // Server / Receiver
    private fun onTransferRequest(packet: Packet) {
        val payload = packet.getPayload(TransferRequestPayload.Deserializer)
        payload.dataSize = senderDataSize ?: 0
        if (packet.source is IPv4Address) {
            // Transfer Request Response
            sendData(
                serializePacket(MessageId.TRANSFER_REQUEST_RESPONSE, payload, sign = false),
                (packet.source as IPv4Address).ip,
                (packet.source as IPv4Address).port,
                payload.port
            )
        }
    }

    /**
     * Respond to client with packet data
     */
    private fun sendData(data: ByteArray, serverIp: String, clientPort: Int, serverPort: Int) {
        try {
            val address = InetAddress.getByName(serverIp)
            DatagramSocket(serverPort).use {
                val packet = DatagramPacket(data, data.size, address, clientPort)
                it.send(packet)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onTransferRequestResponse(packet: Packet) {
        this.serverWanPort = (packet.source as IPv4Address).port
        this.receivedDataSize = packet.getPayload(TransferRequestPayload.Deserializer).dataSize
    }
}
