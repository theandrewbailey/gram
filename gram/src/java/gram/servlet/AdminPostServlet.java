package gram.servlet;

import gram.AdminPermission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.AbstractInput;
import gram.ArticleProcessor;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.bean.GramTenant;

/**
 *
 * @author alpha
 */
@WebServlet(name = "adminPost", description = "Administer articles (and sometimes comments)", urlPatterns = {"/adminPost"})
public class AdminPostServlet extends AdminServlet {

    public static final String ADMIN_EDIT_POSTS = "/WEB-INF/adminEditPosts.jsp";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.EDIT_POSTS};
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        Instant start = Instant.now();
        if (request.getParameter("deletecomment") != null) {      // delete comment
            ten.getComms().delete(Integer.parseInt(request.getParameter("deletecomment")));
            ten.getArts().evict();
            ten.getGlobalCache().clear();
            RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
            showList(request, response, ten.getArts().getAll(null));
        } else if (request.getParameter("disablecomments") != null) {
            List<Article> articles = new ArrayList<>();
            for (String id : request.getParameterValues(AbstractInput.getIncomingHash(request, "selectedArticle"))) {
                Article art = ten.getArts().get(Integer.parseInt(id));
                art.setComments(Boolean.FALSE);
                articles.add(art);
            }
            ten.getArts().upsert(articles);
            ten.getArts().evict();
            ten.getGlobalCache().clear();
            RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
            response.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(request, Boolean.FALSE));
            response.sendRedirect(request.getAttribute(SecurityRepo.BASE_URL).toString());
        } else if (request.getParameter("rewrite") != null) {
//            List<Fileupload> files = Collections.synchronizedList(new ArrayList<>(BackupDaemon.PROCESSING_CHUNK_SIZE * 2));
//            ten.getFile().processArchive((file) -> {
//                String url = BaseFileServlet.getImmutableURL(ten.getImeadValue(SecurityRepo.BASE_URL), file);
//                if (!url.equals(file.getUrl())) {
//                    file.setUrl(url);
//                    files.add(file);
//                }
//                synchronized (files) {
//                    if (files.size() > BackupDaemon.PROCESSING_CHUNK_SIZE) {
//                        final List<Fileupload> fileChunk = new ArrayList<>(files);
//                        ten.getFile().upsert(fileChunk);
//                        files.clear();
//                    }
//                }
//            }, false);
//            if (!files.isEmpty()) {
//                ten.getFile().upsert(files);
//            }
            try {
                Queue<Future<Article>> articleTasks = new ConcurrentLinkedQueue<>();
                for (String id : request.getParameterValues(AbstractInput.getIncomingHash(request, "selectedArticle"))) {
                    Article art = ten.getArts().get(Integer.parseInt(id));
                    art.setPostedhtml(null);
                    art.setImageurl(null);
                    articleTasks.add(ten.getExec().submit(new ArticleProcessor(ten, art)));
                }
                ten.getExec().submit(() -> {
                    List<Article> articles = new ArrayList<>(articleTasks.size());
                    for (Future<Article> f : articleTasks) {
                        while (true) {
                            try {
                                if (articles.isEmpty() || null != f.get(1L, TimeUnit.MILLISECONDS)) {
                                    articles.add(f.get());
                                    break;
                                }
                            } catch (TimeoutException t) {
                                ten.getArts().upsert(articles);
                                articles.clear();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    ten.getArts().upsert(articles);
                    ten.getArts().evict();
                    ten.getGlobalCache().clear();
                });
            } catch (NullPointerException n) {
            }
            ten.getGlobalCache().clear();
            RequestTimer.addTiming(request, "rewrite", Duration.between(start, Instant.now()));
            response.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(request, Boolean.FALSE));
            response.setHeader("Clear-Site-Data", "cache");
            response.sendRedirect(request.getAttribute(SecurityRepo.BASE_URL).toString());
        } else {
            List<Article> articles = ten.getArts().getAll(null);
            RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
            showList(request, response, articles);
        }
    }

    public static void showList(HttpServletRequest request, HttpServletResponse response, Collection<Article> articles) throws ServletException, IOException {
        request.setAttribute("title", "Posts");
        request.setAttribute("articles", articles);
        request.getRequestDispatcher(ADMIN_EDIT_POSTS).forward(request, response);
    }
}
