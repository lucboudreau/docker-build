<#if 0 < results?size >
${header}:
============
  <#list results?keys?sort as group>
    <#assign diffs = results[group]>
    <#list diffs?keys?sort as classname>
${classname}:
      <#assign coverages = diffs[classname]>
      <#list coverages?keys?sort as coverageType>
        <#assign number = coverages[coverageType]>
 * ${coverageType} : <#if coverageType?ends_with("Change") ><#if 0 <= number> + <#else>**</#if></#if>${number?string["#.00"]}%<#if coverageType?ends_with("Change") ><#if 0 <= number><#else>**</#if></#if>
      </#list>

    </#list>
  </#list>
</#if>
