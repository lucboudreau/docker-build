<#macro errorMd className testCase errorType errorMessage>
${className}<#if testCase??>.${testCase}</#if>: ${errorType}
```
${errorMessage}
```
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
<@errorMd className=className testCase=name errorType=classErrors[name].type errorMessage=classErrors[name].message/>
        </#list>
      </#list>
      <#list errors[suiteName].suiteErrors as error>
<@errorMd className=suiteName errorType=error.type errorMessage=error.message/>
      </#list>
    </#list>
  </#if>
</#macro>

<@errorsMd header="Newly Broken Tests" errors=broken/>

<@errorsMd header="Newly Fixed Tests" errors=fixed/>

<@errorsMd header="Still Broken Tests" errors=stillBroken/>

