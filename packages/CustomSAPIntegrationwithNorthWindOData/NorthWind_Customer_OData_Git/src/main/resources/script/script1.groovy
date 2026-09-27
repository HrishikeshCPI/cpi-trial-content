import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def enableLogging = message.getProperty("EnablePayloadLogging")

    if ("true".equalsIgnoreCase(enableLogging?.toString())) {

        def messageLog = messageLogFactory.getMessageLog(message)

        if (messageLog != null) {

            def body = message.getBody(String)

            messageLog.addAttachmentAsString(
                "Response Payload",
                body,
                "text/plain"
            )
        }
    }

    return message
}
