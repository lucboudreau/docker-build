{%- macro errorMd(className, testCase, errorType, errorMessage) -%}
{{ className }}{% if testCase %}.{{ testCase }}{% endif %}: {{ errorType }}
```
{{ errorMessage }}
```
{%- endmacro -%}

{%- macro errorsMd(header, errors) -%}
  {%- if errors|length > 0 -%}
{{ header }}:
-----------------
    {%- for classname, classErrors in errors.iteritems() -%}
      {%- if 'caseErrors' in classErrors -%}
        {% for name, error in classErrors['caseErrors'].iteritems() %}
{{ errorMd(classname, name, error.type, error.message) }}
        {%- endfor -%}
      {%- endif -%}

      {%- if 'suiteErrors' in classErrors -%}
        {% for error in classErrors['suiteErrors'] %}
{{ errorMd(classname, None, error.type, error.message) }}
        {%- endfor -%}
      {%- endif -%}
    {%- endfor -%}
  {%- endif -%}
{%- endmacro %}
{{ errorsMd('Newly Broken Tests', broken)  }}

{{ errorsMd('Newly Fixed Tests', fixed)  }}

{{ errorsMd('Still Broken Tests', stillBroken)  }}

