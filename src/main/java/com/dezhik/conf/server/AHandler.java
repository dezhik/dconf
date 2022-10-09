package com.dezhik.conf.server;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AHandler extends AbstractHandler {

    // when mongodb is down
    // com.mongodb.MongoSocketReadException: Prematurely reached end of stream

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected String extractStringParam(HttpServletRequest request, String name) {
        String[] values = request.getParameterMap().get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    protected Long extractLongParam(HttpServletRequest request, String name) {
        String str = extractStringParam(request, name);
        if (str == null) return null;

        try {
            return Long.parseLong(str);
        } catch (NumberFormatException nfe) { }

        return null;
    }
}
