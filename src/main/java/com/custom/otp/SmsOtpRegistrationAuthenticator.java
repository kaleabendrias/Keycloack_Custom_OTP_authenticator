package com.custom.otp;

import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class SmsOtpRegistrationAuthenticator implements Authenticator {

    // private final KafkaProducer<String, String> producer;

    // public SmsOtpRegistrationAuthenticator(KafkaProducer<String, String>
    // producer) {
    // this.producer = producer;
    // }

    public static final String OTP_NOTE = "sms_registration_otp";
    public static final String OTP_SENT = "sms_registration_otp_sent";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        UserModel user = context.getUser();

        if (user == null) {
            // get user from session if not in context
            user = authSession.getAuthenticatedUser();
        }

        if (user == null) {
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        // block them from accessing the page if they didnt complete OTP verification
        // user.setEnabled(true);
        if (!"true".equals(authSession.getAuthNote(OTP_SENT))) {
            user.addRequiredAction("unverified-user-block");
            user.setSingleAttribute("phone_verified", "false");
        }

        // CHECK IF RESUMING
        if ("true".equals(authSession.getAuthNote(OTP_SENT))) {
            context.challenge(context.form().createForm("sms-otp-form.ftl"));
            return;
        }

        // SEND OTP
        String phone = user.getFirstAttribute("phone_number");
        if (phone == null || phone.isBlank()) {
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        String request_id = authSession.getAuthNote("request_id");
        if (request_id == null) {
            request_id = UUID.randomUUID().toString();
            authSession.setAuthNote("request_id", request_id);
        }

        String otp = generateOtp();
        sendOtp(phone, otp, request_id);

        authSession.setAuthNote(OTP_NOTE, otp);
        authSession.setAuthNote(OTP_SENT, "true");
        authSession.setAuthNote("created_at", String.valueOf(System.currentTimeMillis()));

        context.challenge(context.form().createForm("sms-otp-form.ftl"));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String inputOtp = formData.getFirst("otp_code");
        String storedOtp = context.getAuthenticationSession().getAuthNote(OTP_NOTE);

        long expiryTimeMs = 1 * 60 * 1000;

        boolean isExpired = (System.currentTimeMillis()
                - Long.parseLong(context.getAuthenticationSession().getAuthNote("created_at"))) > expiryTimeMs;

        if (isExpired) {
            // retry if OTP expired
            context.getAuthenticationSession().removeAuthNote(OTP_NOTE);
            context.getAuthenticationSession().removeAuthNote(OTP_SENT);
            // resend OTP again with button resend added
            context.challenge(
                    context.form()
                            .setError("OTP expired")
                            .createForm("sms-otp-form.ftl"));
            return;
        }

        if (storedOtp == null || !storedOtp.equals(inputOtp)) {
            // Retry if they give wrong OTP
            context.challenge(
                    context.form()
                            .setError("Invalid OTP code")
                            .createForm("sms-otp-form.ftl"));
            return;
        }

        // SUCCESS
        UserModel user = context.getUser();
        if (user == null) {
            user = context.getAuthenticationSession().getAuthenticatedUser();
        }

        if (user != null) {
            user.setSingleAttribute("phone_verified", "true");

            // remove login restrictions
            user.removeRequiredAction("unverified-user-block");
            // user.setEmailVerified(true); // to be marked if i trust the flow more
            // user.setEnabled(true);
        }

        context.getAuthenticationSession().removeAuthNote(OTP_NOTE);
        context.getAuthenticationSession().removeAuthNote(OTP_SENT);

        context.success();
    }

    // Run even if user is disabled
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {
    }

    @Override
    public void close() {
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void sendOtp(String phone, String otp, String request_id) {
        // org.jboss.logging.Logger.getLogger(SmsOtpRegistrationAuthenticator.class)
        // .infof("Sending OTP to %s: %s", phone, otp);
        // Properties props = new Properties();
        // props.put("bootstrap.servers",
        // System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        // props.put("key.serializer",
        // "org.apache.kafka.common.serialization.StringSerializer");
        // props.put("value.serializer",
        // "org.apache.kafka.common.serialization.StringSerializer");
        // props.put("acks", System.getenv().getOrDefault("KAFKA_ACKS", "all"));
        // props.put("request.timeout.ms",
        // System.getenv().getOrDefault("KAFKA_REQUEST_TIMEOUT_MS", "30000"));
        // props.put("retries", System.getenv().getOrDefault("KAFKA_RETRIES", "3"));

        // try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
        // producer.send(new
        // ProducerRecord<>(System.getenv().getOrDefault("KAFKA_OTP_TOPIC",
        // "otp_registrations"),
        // request_id, phone + ":" + otp)); // obj
        // } catch (Exception e) {
        // org.jboss.logging.Logger.getLogger(getClass())
        // .error("Failed to publish OTP to Kafka", e);
        // } // self contained keycloak deployment and env't vars
        // internally kafka connects to sms service

        KafkaProducerProvider.getInstance()
                .send(new ProducerRecord<>(System.getenv().getOrDefault("KAFKA_OTP_TOPIC", "otp_registrations"),
                        request_id, phone + ":" + otp));
    }
}