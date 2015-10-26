Changed files:
==============
{%- for file in fileList %}
  {{file}}
{%- endfor %}

Checkstyle violations:
======================
{%- for violation in violations %}
File {{ violation.filename }}
{% for error in violation.errors -%}
{{ loop.index }}. Message: {{error.message}} at Line: {{ error.line }} Column: {{ error.column }}
{% endfor -%}
{%- else %}
No errors found
{%- endfor -%}
