package com.vaultsandbox.testhelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaultsandbox.client.Attachment;
import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.model.ExportedInbox;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length < 1) {
            fatal("usage: testhelper <command> [args]");
        }

        String command = args[0];

        try (VaultSandboxClient client = VaultSandboxClient.create(
                ClientConfig.builder()
                    .apiKey(System.getenv("VAULTSANDBOX_API_KEY"))
                    .baseUrl(System.getenv("VAULTSANDBOX_URL"))
                    .build())) {

            switch (command) {
                case "create-inbox":
                    createInbox(client);
                    break;
                case "import-inbox":
                    importInbox(client);
                    break;
                case "read-emails":
                    readEmails(client);
                    break;
                case "cleanup":
                    if (args.length < 2) {
                        fatal("usage: testhelper cleanup <address>");
                    }
                    cleanup(client, args[1]);
                    break;
                default:
                    fatal("unknown command: " + command);
            }
        } catch (Exception e) {
            fatal(e.getMessage());
        }
    }

    private static void createInbox(VaultSandboxClient client) throws Exception {
        Inbox inbox = client.createInbox();
        ExportedInbox exported = inbox.export();

        ObjectNode output = mapper.createObjectNode();
        output.put("version", exported.getVersion());
        output.put("emailAddress", exported.getEmailAddress());
        output.put("expiresAt", exported.getExpiresAt());
        output.put("inboxHash", exported.getInboxHash());
        output.put("serverSigPk", exported.getServerSigPk());
        output.put("secretKey", exported.getSecretKey());
        output.put("exportedAt", exported.getExportedAt());

        System.out.println(mapper.writeValueAsString(output));
    }

    private static void importInbox(VaultSandboxClient client) throws Exception {
        String input = readStdin();
        ExportedInbox exportData = mapper.readValue(input, ExportedInbox.class);

        client.importInbox(exportData);

        ObjectNode output = mapper.createObjectNode();
        output.put("success", true);
        System.out.println(mapper.writeValueAsString(output));
    }

    private static void readEmails(VaultSandboxClient client) throws Exception {
        String input = readStdin();
        ExportedInbox exportData = mapper.readValue(input, ExportedInbox.class);

        Inbox inbox = client.importInbox(exportData);
        List<Email> emailList = inbox.listEmails();

        ObjectNode output = mapper.createObjectNode();
        ArrayNode emailsArray = output.putArray("emails");

        for (Email email : emailList) {
            ObjectNode emailNode = mapper.createObjectNode();
            emailNode.put("id", email.getId());
            emailNode.put("subject", email.getSubject());
            emailNode.put("from", email.getFrom());

            ArrayNode toArray = emailNode.putArray("to");
            for (String to : email.getTo()) {
                toArray.add(to);
            }

            emailNode.put("text", email.getText() != null ? email.getText() : "");
            emailNode.put("html", email.getHtml() != null ? email.getHtml() : "");

            ArrayNode attachmentsArray = emailNode.putArray("attachments");
            if (email.getAttachments() != null) {
                for (Attachment att : email.getAttachments()) {
                    ObjectNode attNode = mapper.createObjectNode();
                    attNode.put("filename", att.getFilename());
                    attNode.put("contentType", att.getContentType());
                    attNode.put("size", att.getSize());
                    attachmentsArray.add(attNode);
                }
            }

            emailNode.put("receivedAt", email.getReceivedAt().toString());
            emailsArray.add(emailNode);
        }

        System.out.println(mapper.writeValueAsString(output));
    }

    private static void cleanup(VaultSandboxClient client, String address) throws Exception {
        client.deleteInbox(address);

        ObjectNode output = mapper.createObjectNode();
        output.put("success", true);
        System.out.println(mapper.writeValueAsString(output));
    }

    private static String readStdin() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void fatal(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
