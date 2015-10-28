Build Commands:
==============
Build:
------
{% for command in build -%}
{{ loop.index }}. {{ command }}
{% endfor %}
{% if finally -%}
Cleanup:
-------
{{ finally }}
{% endif %}
