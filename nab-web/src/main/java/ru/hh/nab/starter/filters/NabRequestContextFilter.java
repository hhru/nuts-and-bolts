package ru.hh.nab.starter.filters;

import org.springframework.web.filter.RequestContextFilter;
import ru.hh.nab.common.servlet.SystemFilter;

public class NabRequestContextFilter extends RequestContextFilter implements SystemFilter {
}
