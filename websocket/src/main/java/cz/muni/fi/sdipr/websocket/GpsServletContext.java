package cz.muni.fi.sdipr.websocket;

import cz.muni.fi.sdipr.api.factories.AuthManagerFactory;
import cz.muni.fi.sdipr.api.factories.SubscriptionManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class GpsServletContext implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(GpsServletContext.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("Context CREATED...");
        KafkaAuthService.initialize(AuthManagerFactory.getInstance(), SubscriptionManagerFactory.getInstance());
        KafkaGpsService.initialize(SubscriptionManagerFactory.getInstance());
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        logger.info("Context DESTROYED...");
        KafkaAuthService.stop();
        KafkaGpsService.stop();
    }
}
