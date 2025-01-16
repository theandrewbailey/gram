package libWebsiteTools.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import jakarta.ejb.EJB;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.Landlord;

/**
 *
 * @author alpha
 */
//@WebServlet(name = "SpinnerServlet", urlPatterns = {"/spin"}, asyncSupported = true)
public class SpinnerServlet{
//    extends BaseServlet {
//
    public static final String REASON = "$_SPINNER_REASON";
//    @EJB
//    protected Landlord allBeans;
//
//    public static class Whirr implements WriteListener, Runnable {
//
//        private final ServletOutputStream writer;
//        private final AsyncContext ac;
//        private final Landlord beans;
//        private byte[] LEADER = "<!DOCTYPE html>\n<html><head></head><body>".getBytes();
//        private final byte[] SPINNER;
//        private volatile long sent = 0L;
//        private volatile Thread running;
//
//        public Whirr(ServletOutputStream sos, AsyncContext context, Landlord beans) {
//            this.writer = sos;
//            this.ac = context;
//            this.beans = beans;
//            StringBuilder temp = new StringBuilder(3000);
//            for (int x = 0; x < 1000; ++x) {
//                temp.append("<a>");
//            }
//            SPINNER = temp.toString().getBytes();
//        }
//
//        @Override
//        public void onWritePossible() throws IOException {
//            running = Thread.currentThread();
//            beans.getExec().execute(this);
//            if (writer.isReady() && null != LEADER) {
//                writer.write(LEADER);
//                sent += LEADER.length;
//                LEADER = null;
//            }
//            while (writer.isReady()) {
//                writer.write(SPINNER);
//                sent += SPINNER.length;
//            }
//        }
//
//        @Override
//        public void onError(Throwable t) {
//            running = Thread.currentThread();
//            ac.complete();
//            HttpServletRequest req = (HttpServletRequest) ac.getRequest();
//            GuardFilter.kill(req, (HttpServletResponse) ac.getResponse());
//            OffsetDateTime localNow = RequestTimer.getStartTime(req);
//            long elapsed = OffsetDateTime.now().getLong(ChronoField.INSTANT_SECONDS) - localNow.getLong(ChronoField.INSTANT_SECONDS);
//            SecurityRepo error = beans.getError();
//            String reason = req.getAttribute(REASON).toString();
//            if (null != reason) {
//                error.putInHoneypot(SecurityRepo.getIP(req));
//                error.logException(req,
//                        SecurityRepo.getIP(req) + ' ' + req.getMethod() + ' ' + AbstractInput.getTokenURL(req),
//                        reason + ", sent " + sent + " bytes over " + elapsed + " seconds.", t);
//            } else {
//                error.logException(req,
//                        SecurityRepo.getIP(req) + ' ' + req.getMethod() + ' ' + AbstractInput.getTokenURL(req),
//                        "Sent " + sent + " bytes over " + elapsed + " seconds.", t);
//            }
//            running = null;
//        }
//
//        @Override
//        @SuppressWarnings("SleepWhileInLoop")
//        public void run() {
//            try {
//                Thread.sleep(1000);
//                long old = sent;
//                Thread.sleep(1000);
//                while (null != running) {
//                    if (old == sent) {
//                        running.interrupt();
//                    }
//                    old = sent;
//                    Thread.sleep(1000);
//                }
//            } catch (InterruptedException ex) {
//                if (null != running) {
//                    running.interrupt();
//                }
//            }
//        }
//    }
//
//    protected void serve(HttpServletRequest req, HttpServletResponse res) throws IOException {
//        Landlord beans = allBeans.getInstance(req);
//        /*res.setBufferSize(3000);
//        res.setContentType("text/html");
//        if (JspFilter.GZIP.equals(JspFilter.getCompression(req))) {
//            ServletOutputWrapper<ServletOutputWrapper.GZIPOutput> gzWrap = new ServletOutputWrapper<>(ServletOutputWrapper.GZIPOutput.class,
//                    (HttpServletResponse) res);
//            req.startAsync(req, gzWrap);
//            gzWrap.getOutputStream().setWriteListener(new Whirr(gzWrap.getOutputStream(), req.getAsyncContext(), beans));
//        } else {
//            req.startAsync(req, res);
//            res.getOutputStream().setWriteListener(new Whirr(res.getOutputStream(), req.getAsyncContext(), beans));
//        }*/
//        String reason = req.getAttribute(REASON).toString();
//        if (null != reason) {
//            beans.getError().logException(req, SecurityRepo.getIP(req) + ' ' + req.getMethod() + ' ' + AbstractInput.getTokenURL(req),
//                    reason, null);
//        }
//        killInHoney(req, res);
//    }
//
//    public boolean killInHoney(HttpServletRequest req, HttpServletResponse res) {
//        GuardFilter.kill(req, res);
//        return allBeans.getInstance(req).getError().putInHoneypot(SecurityRepo.getIP(req));
//    }
//
//    @Override
//    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//        Landlord beans = allBeans.getInstance(request);
//        String reason = request.getAttribute(REASON).toString();
//        if (null != reason) {
//            beans.getError().logException(request, SecurityRepo.getIP(request) + ' ' + request.getMethod() + ' ' + AbstractInput.getTokenURL(request),
//                    reason, null);
//        } else {
//            beans.getError().logException(request, SecurityRepo.getIP(request) + ' ' + request.getMethod() + ' ' + AbstractInput.getTokenURL(request),
//                    reason, null);
//        }
//        killInHoney(request, response);
//    }
}
