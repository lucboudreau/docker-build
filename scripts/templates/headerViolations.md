{%- if violations|length > 0 %}
License header violations:
==========================
{% for violation in violations %}
{{ loop.index }}. {{ violation.file }}
{% endfor -%}
{%- endif -%}
