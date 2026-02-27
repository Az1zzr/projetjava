package Tunescom.TunescomJAVA.tools;

import java.lang.reflect.*;
import java.sql.Connection;

public class ConnectionWrapper {
    private final Connection realConnection;

    public ConnectionWrapper(Connection realConnection) {
        this.realConnection = realConnection;
    }

    public Connection getWrappedConnection() {
        return (Connection) Proxy.newProxyInstance(
                realConnection.getClass().getClassLoader(),
                new Class[]{Connection.class},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("close".equals(method.getName())) {
                            System.out.println("❗ Appel à close() détecté !");
                            printCallerLocation();
                        }
                        return method.invoke(realConnection, args);
                    }
                });
    }

    private void printCallerLocation() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(stackTrace.length, 8); i++) {
            System.out.println("   ↪ " + stackTrace[i]);
        }
    }
}
