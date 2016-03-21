<#macro errorMd className errors testCase="">
${className}<#if testCase?has_content>.${testCase}</#if>:
-----------------------------------------------
<#list errors as error>
${error.type}
```
${error.message}
```
</#list>
</#macro>

<#macro errorsMd header errors>
  <#if errors?? && 0 < errors?size >
${header}:
======================
    <#list errors?keys?sort as suiteName>
      <#assign caseErrors = errors[suiteName].caseErrors>
      <#list caseErrors?keys?sort as className>
        <#assign classErrors = caseErrors[className]>
        <#list classErrors?keys?sort as name>
<@errorMd className=className testCase=name errors=classErrors[name]/>
        </#list>
      </#list>
      <#if 0 < errors[suiteName].suiteErrors?size >
<@errorMd className=suiteName errors=errors[suiteName].suiteErrors/>
      </#if>
    </#list>
  </#if>
</#macro>

<@errorsMd header="Newly Broken Tests" errors=broken/>

<@errorsMd header="Newly Fixed Tests" errors=fixed/>

<@errorsMd header="Still Broken Tests" errors=stillBroken/>

