{%- if results|length > 0 -%}
{{ header }}:
============
  {%- for group, diffs in results.iteritems() -%}
    {%- for classname, coverages in diffs.iteritems() %}
{{ classname }}:
      {%- for coverageType, number in coverages.iteritems() %}
 * {{ coverageType }} : 
        {%- if coverageType.endswith('Change') and number >= 0 %} +
        {%- elif coverageType.endswith('Change') and number < 0 %}**
        {%- endif -%}
        {{ '{0:.2f}'.format(number) }}%
        {%- if coverageType.endswith('Change') and number < 0 -%}**
        {%- endif %}
      {%- endfor %}
    {% endfor -%}
  {%- endfor -%}
{%- endif -%}
