package libWebsiteTools.security;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

/**
 *
 * @author alpha
 */
@WebServlet(name = "CSP Reporter", description = "Receives and logs Content Security Policy reports", urlPatterns = {"/report"})
public class CSPReporter extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        StringBuilder report = new StringBuilder(1000).append("IP: ").append(SecurityRepo.getIP(request)).append(SecurityRepo.NEWLINE);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                String header = headers.nextElement();
                report.append(headerName).append(": ").append(SecurityRepo.htmlFormat(header)).append(SecurityRepo.NEWLINE);
            }
        }
        report.append(SecurityRepo.NEWLINE).append(SecurityRepo.NEWLINE).append("csp-report:").append(SecurityRepo.NEWLINE);
        JsonReader read = Json.createReader(request.getInputStream());
        JsonObject reportObject = read.readObject().getJsonObject("csp-report");
        for (Map.Entry<String, JsonValue> field : reportObject.entrySet()) {
            report.append(SecurityRepo.htmlFormat(field.getKey())).append(": ").append(SecurityRepo.htmlFormat(field.getValue().toString())).append(SecurityRepo.NEWLINE);
        }
        Tenant ten = Landlord.getTenant(request);
        ten.getError().logException(null, "Content Security Policy violation", report.toString(), null);
    }
}
