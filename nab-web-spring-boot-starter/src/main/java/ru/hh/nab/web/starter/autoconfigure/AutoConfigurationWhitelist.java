package ru.hh.nab.web.starter.autoconfigure;

import org.springframework.boot.context.annotation.ImportCandidates;

/**
 * This annotation is for internal use only - annotation is passed to {@link ImportCandidates#load(Class, ClassLoader)} in order to load auto
 * configurations whitelist from {@code ru.hh.nab.autoconfigure.AutoConfigurationWhitelist.imports} file.
 */
public @interface AutoConfigurationWhitelist {
}
