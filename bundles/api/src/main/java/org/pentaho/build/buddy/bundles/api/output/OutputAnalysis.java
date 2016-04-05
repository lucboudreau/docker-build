package org.pentaho.build.buddy.bundles.api.output;

/**
 * Describes a
 * Created by bryan on 2/16/16.
 */
public interface OutputAnalysis {
    /**
     * The severity status for this analysis.
     */
    OutputSeverity getOutputSeverity();

    /**
     * The full analysis report, in Markdown form.
     */
    String getReport();

    /**
     * The URL (if any) to add for more details about this analysis.
     * Usually a help page or a build console.
     */
    String getURL();

    /**
     * A builder for analysis results.
     */
    public static class Builder {
        private OutputSeverity sev;
        private String report;
        private String url;
        public Builder severity( OutputSeverity sev ) {
            this.sev = sev;
            return this;
        }
        public Builder report( String report ) {
            this.report = report;
            return this;
        }
        public Builder url( String url ) {
            this.url = url;
            return this;
        }
        /**
         * Builds and returns an immutable OutputAnalysis implementation.
         */
        public OutputAnalysis build () {
            return new OutputAnalysis() {
                private final String m_report = report;
                private final String m_url = url;
                private final OutputSeverity m_sev = sev;
                public OutputSeverity getOutputSeverity() {
                    return m_sev;
                }
                public String getReport() {
                    return m_report;
                }
                public String getURL() {
                    return m_url;
                }
            };
        }
    }
}
