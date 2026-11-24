package com.gupta.linkly.config;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;

/**
 * Pays Java's one-time TLS setup cost (loading crypto providers, parsing the CA trust store,
 * generating a key share) BEFORE the app opens its first database connection.
 *
 * <p>Why: on very small cloud instances (Render free tier ≈ 0.1 CPU) this setup can take
 * seconds. If it happens in the middle of the handshake with Supabase's connection pooler, the
 * pooler may give up and close the connection ("Remote host terminated the handshake").
 * The printed duration also tells us, from the deploy logs, how slow the machine is.
 */
public final class TlsWarmup {

    private TlsWarmup() {
    }

    public static void run() {
        long start = System.nanoTime();
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null); // default key/trust managers: loads cacerts
            SSLEngine engine = context.createSSLEngine("warmup.invalid", 443);
            engine.setUseClientMode(true);
            engine.beginHandshake();
            ByteBuffer out = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
            engine.wrap(ByteBuffer.allocate(0), out); // builds a real ClientHello
            System.out.printf("TLS warm-up done in %d ms%n", (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            System.out.println("TLS warm-up skipped: " + e);
        }
    }
}
