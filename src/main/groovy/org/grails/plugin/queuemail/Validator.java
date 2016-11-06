package org.grails.plugin.queuemail;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.Socket;
import java.util.*;
import org.apache.log4j.Logger;
import java.util.regex.Pattern;

/**
 * Validator for mailbox
 *
 * @author Vitalii Samolovskikh aka Kefir - isMailbox
 * @author Vahid Hedayati - added isMailboxAndResolves
 */
public class Validator {

    private static Logger log = Logger.getLogger(String.valueOf(Validator.class));

    private static final String HEADER = "QueueMail Email: - ";

    private static final Pattern EMAIL_PATTERN = java.util.regex.Pattern
            .compile("^[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");

    public static boolean isMailbox(String value) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(value);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    public static boolean isMailboxAndResolves(String fromAddress, String value, boolean smtpValidation) throws NamingException {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(value);
            emailAddr.validate();
            String address = emailAddr.getAddress();
            InternetAddress fromAddrr = new InternetAddress(fromAddress);
            fromAddrr.validate();
            //final Matcher matcher = EMAIL_PATTERN.matcher(address);
            //if (!matcher.find()) {
            //   return false;
            //}

            if (smtpValidation) {
                // This will attempt to validate email address from last MX bound to domain
                // If your port 25 is blocked - all UK broadband home connections are
                // then disable queueMail.smtpValidation by not configuring or setting to false
                result = isAddressValid(fromAddrr.getAddress(), emailAddr.getAddress());
            } else {
                int pos = address.indexOf('@');
                if (pos == -1) {
                    return false;
                }
                String domain = address.substring(++pos);
                result =isValidMx(domain);
            }
        } catch (AddressException ex) {
            result = false;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    static boolean isValidMx( String hostName) throws NamingException {
        boolean result=true;
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial",
                    "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(hostName, new String[] { "MX" });
            Attribute attr = attrs.get( "MX" );
            if( attr == null ) {
                result=false;
            }
        } catch (Exception e) {
            result=false;
        }
        return result;
    }


    /**
     *
     * Write the text ot buffer.
     *
     * @param wr
     * @param text
     * @throws IOException
     */
    private static void write(BufferedWriter wr, String text) throws IOException {
        wr.write(text + "\r\n"); //$NON-NLS-1$
        wr.flush();
    }

    private static int getResponse(BufferedReader in) throws IOException {
        String line = null;
        int res = 0;
        while ((line = in.readLine()) != null) {
            String pfx = line.substring(0, 3);
            try {
                res = Integer.parseInt(pfx);
            } catch (Exception ex) {
                res = -1;
            }
            if (line.charAt(3) != '-') {
                break;
            }
        }

        return res;
    }

    private static List<String> getMX(String hostName) throws NamingException {
        // Perform a DNS lookup for MX records in the domain
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial",
                "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = new InitialDirContext(env);
        Attributes attrs = ictx.getAttributes(hostName, new String[] { "MX" }); //$NON-NLS-1$
        Attribute attr = attrs.get("MX"); //$NON-NLS-1$
        List<String> res = new ArrayList<String>();
        // if we don't have an MX record, try the machine itself
        if ((attr == null) || (attr.size() == 0)) {
            attrs = ictx.getAttributes(hostName, new String[] { "A" }); //$NON-NLS-1$
            attr = attrs.get("A"); //$NON-NLS-1$
            if (attr == null) {
                if (log.isInfoEnabled()) {
                   log.info( "No match for hostname '" + hostName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return res;
            }
        }
        // we have machines to try. Return them as an array list
        NamingEnumeration<?> en = attr.getAll();
        Map<Integer, String> map = new TreeMap<Integer, String>();

        while (en.hasMore()) {

            String mailhost;
            String x = (String) en.next();
            String f[] = x.split(" "); //$NON-NLS-1$
            Integer key = 0;
            if (f.length == 1) {
                mailhost = f[0];
            } else if (f[1].endsWith(".")) { //$NON-NLS-1$
                mailhost = f[1].substring(0, (f[1].length() - 1));
                key = Integer.valueOf(f[0]);
            } else {
                mailhost = f[1];
                key = Integer.valueOf(f[0]);
            }
            map.put(key, mailhost);
        }
        // NOTE: We SHOULD take the preference into account to be absolutely
        // correct.
        Iterator<Integer> keyInterator = map.keySet().iterator();
        while (keyInterator.hasNext()) {
            res.add(map.get(keyInterator.next()));
        }
        return res;
    }

    static boolean isAddressValid(String fromAddress, String address) {
        if (address == null) {
            return false;
        }
        // Find the separator for the domain name
        int pos = address.indexOf('@');

        // If the address does not contain an '@', it's not valid
        if (pos == -1) {
            return false;
        }

        // Isolate the domain/machine name and get a list of mail exchangers
        String domain = address.substring(++pos);
        List<String> mxList = null;
        try {
            mxList = getMX(domain);
        } catch (NamingException ex) {
            return false;
        }
        // Just because we can send mail to the domain, doesn't mean that the
        // address is valid, but if we can't, it's a sure sign that it isn't
        if (mxList.size() == 0) {
            return false;
        }

        // Now, do the SMTP validation, try each mail exchanger until we get
        // a positive acceptance. It *MAY* be possible for one MX to allow
        // a message [store and forwarder for example] and another [like
        // the actual mail server] to reject it. This is why we REALLY ought
        // to take the preference into account.
        for (int mx = 0; mx < mxList.size(); mx++)
            try {
                int res;
                Socket skt = new Socket(mxList.get(mx), 25);
                //Socket skt = new Socket("localhost", 25);
                BufferedReader rdr = new BufferedReader(new InputStreamReader(skt.getInputStream()));
                BufferedWriter wtr = new BufferedWriter(new OutputStreamWriter(skt.getOutputStream()));

                res = getResponse(rdr);
                if (res != 220) { // SMTP Service ready.
                    if (log.isInfoEnabled()) {
                       log.info(HEADER + "Invalid header:" + mxList.get(mx)); //$NON-NLS-1$
                    }

                    return false;
                }
                write(wtr, "EHLO " + address.substring(fromAddress.indexOf("@") + 1)); //$NON-NLS-1$  //$NON-NLS-2$

                res = getResponse(rdr);
                if (res != 250) {
                    if (log.isInfoEnabled()) {
                         log.info(HEADER + "Not ESMTP: " + fromAddress.substring(fromAddress.indexOf("@") + 1)); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    return false;
                }

                // validate the sender address
                write(wtr, "MAIL FROM: <" + fromAddress + ">"); //$NON-NLS-1$//$NON-NLS-2$
                res = getResponse(rdr);
                if (res != 250) {
                    if (log.isInfoEnabled()) {
                         log.info(HEADER + "Sender rejected: " + fromAddress); //$NON-NLS-1$
                    }
                    return false;
                }

                write(wtr, "RCPT TO: <" + address + ">"); //$NON-NLS-1$//$NON-NLS-2$
                res = getResponse(rdr);

                // be polite
                write(wtr, "RSET"); //$NON-NLS-1$
                getResponse(rdr);
                write(wtr, "QUIT"); //$NON-NLS-1$
                getResponse(rdr);
                if (res != 250) {
                    if (log.isInfoEnabled()) {
                        log.info(HEADER + "Address is not valid: " + address); //$NON-NLS-1$
                    }
                    return false;
                }

                rdr.close();
                wtr.close();
                skt.close();
                return true;
            } catch (Throwable e) {
                 if (log.isDebugEnabled()) {
                     log.debug("Connection to " + mxList.get(mx) + " failed.", e); //$NON-NLS-1$ //$NON-NLS-2$
                 }
                continue;
            }
        return false;
    }
}
