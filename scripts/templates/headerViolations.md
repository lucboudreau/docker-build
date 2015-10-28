
License header violations:
==========================
{%- for violation in violations %}
{{ loop.index }}. {{ violation.file }}
{%- else %}
No violations found
{% endfor -%}

