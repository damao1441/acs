package com.ge.predix.acs.request.audit;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest(request);
        System.out.println(IOUtils.toString(multiReadRequest.getInputStream()));
        System.out.println("Status before is: " + response.getStatus());
        filterChain.doFilter(multiReadRequest, response);
        System.out.println("Status after is: " + response.getStatus());
    }
}
