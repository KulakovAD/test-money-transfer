package email.kulakov.test.moneytransfer;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import email.kulakov.test.moneytransfer.api.AccountResource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;

public class MoneyTransferContextListener extends GuiceResteasyBootstrapServletContextListener {

    @Override
    protected List<? extends Module> getModules(ServletContext context) {
        return Collections.singletonList(new ApiModule());
    }

    public static final class ApiModule extends AbstractModule {
        @Override
        public void configure() {
            bind(AccountResource.class);
        }
    }
}
