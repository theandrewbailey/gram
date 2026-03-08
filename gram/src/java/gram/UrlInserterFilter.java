package gram;

import java.io.IOException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.turbo.CompressedOutput;
import libWebsiteTools.turbo.CompressedServletWrapper;

/**
 *
 * @author alpha
 */
@WebFilter(filterName = "UrlInserterFilter", urlPatterns = {"*.jsp"}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class UrlInserterFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = ((HttpServletRequest) request);
        String url = req.getRequestURL().toString();
        if (url.contains("/WEB-INF/admin/")) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse res = (HttpServletResponse) response;
            CompressedOutput.None byteOutput = new CompressedOutput.None();
            CompressedServletWrapper wrap = new CompressedServletWrapper(byteOutput, res);
            wrap.getOutputStream().getOutputStream(res);
            chain.doFilter(request, wrap);

            wrap.flushBuffer();
            ByteArrayOutputStream byteStr = byteOutput.getOutputStream(res);
            String stringOut = byteStr.toString("UTF-8");
            String baseUrl = req.getAttribute(SecurityRepository.BASE_URL).toString();
            String withUrls = stringOut.replaceAll(" href=\"article/", " href=\"" + baseUrl + "article/")
                    .replaceAll(" href=\"index/", " href=\"" + baseUrl + "index/");
            byte[] bytes = withUrls.getBytes("UTF-8");
            response.getOutputStream().write(bytes);
        }
    }
}
