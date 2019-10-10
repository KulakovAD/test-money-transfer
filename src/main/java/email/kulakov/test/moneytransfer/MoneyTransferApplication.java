package email.kulakov.test.moneytransfer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import email.kulakov.test.moneytransfer.db.ConcurrentDatastore;
import email.kulakov.test.moneytransfer.db.Datastore;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public final class MoneyTransferApplication {

    private static final int PORT = 8080;
    private static final Class<? extends Datastore> DATASTORE_CLASS = ConcurrentDatastore.class;

    public static Server createServer(Injector injector, int port)
    {
        final ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");

        final ServletHolder servletHolder = new ServletHolder(new HttpServletDispatcher());
        servletContextHandler.addServlet(servletHolder, "/*");

        final MoneyTransferContextListener contextListener = injector.getInstance(MoneyTransferContextListener.class);
        servletContextHandler.addEventListener(contextListener);

        final Server server = new Server(port);
        server.setHandler(servletContextHandler);

        return server;
    }

    public static void main(String[] args) throws Exception {
        final Injector injector = Guice.createInjector(new MoneyTransferGuiceModule(DATASTORE_CLASS));
        final Server server = createServer(injector, PORT);
        server.start();
        server.join();
    }
}
