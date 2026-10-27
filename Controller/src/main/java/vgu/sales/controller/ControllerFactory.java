package vgu.sales.controller;

import vgu.sales.database.PgStaffDao;

/**
 * Factory class responsible for instantiating and wiring controller services with database DAOs.
 * This class is the only place in the Controller tier that instantiates concrete Database objects.
 *
 * Public: entry point called by the UI tier to obtain controller services.
 */
public final class ControllerFactory {

    private static AuthService authService;

    /**
     * Private constructor to prevent direct instantiation.
     * Private: factory methods are static.
     */
    private ControllerFactory() {
    }

    /**
     * Returns the singleton instance of the AuthService, lazily creating it on first access.
     *
     * @return the shared AuthService instance
     * Public: called by UI application and dialogs to perform authentication.
     */
    public static synchronized AuthService authService() {
        if (authService == null) {
            authService = new AuthServiceImpl(new PgStaffDao());
        }
        return authService;
    }
}
