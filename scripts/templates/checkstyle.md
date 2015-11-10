Changed files:
==============
{%- for file in fileList %}
  {{file}}
{%- endfor %}

{% if violations|length > 0 %}
Checkstyle violations:
======================
See: https://github.com/pentaho/pentaho-coding-standards
{% for violation in violations %}
File {{ violation.filename }}
{% for error in violation.errors -%}
{{ loop.index }}. Message: {{error.message}} at Line: {{ error.line }} Column: {{ error.column }}
{% endfor -%}
{%- endfor -%}
{%- endif -%}
