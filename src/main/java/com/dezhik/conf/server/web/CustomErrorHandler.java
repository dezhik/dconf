package com.dezhik.conf.server.web;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;
import java.io.Writer;

public class CustomErrorHandler extends ErrorHandler {

    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {

    }
}
