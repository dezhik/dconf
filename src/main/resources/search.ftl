<#import "page.ftl" as page>
<@page.header/>

<h3>${title!''}</h3>

<#if emptyResults()>
    <span>Nothing found</span>
<#elseif viewExactProperty()>
    <table class="table table-striped">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Module</th>
            <th scope="col">Host</th>
            <th scope="col">Name</th>
            <th scope="col">Value</th>
            <th scope="col"></th>
        </tr>
        </thead>
        <tbody>
        <#list properties as prop>
        <tr >
            <th scope="row">${prop.module}</th>
            <td>${prop.host}</td>
            <td>${prop.name}</td>
            <td>${prop.valueWithBreaklines}</td>
            <!--    <td><pre>${prop.value}</pre></td>-->
            <td></td>
            <td>
                <a href="/edit?m=${prop.module}&h=${prop.host}&n=${prop.name}"><span class="material-icons md-dark md-18">edit</span></a>
                <span class="material-icons md-dark md-18 ml-4">delete</span>
                <span>(v. ${prop.version})</span>
            </td>
        </tr>
        </#list>
        </tbody>
    </table>
<#else>
    <!-- view list of grouped properties with shortcuts -->
    <@page.decoratedList properties=properties/>
</#if>
        <!--<div class="input-group mb-3">-->
        <!--    <input type="text" class="form-control" placeholder="Recipient's username" aria-label="Recipient's username" aria-describedby="button-addon2">-->
        <!--    <button class="btn btn-outline-secondary" type="button" id="button-addon2">Button</button>-->
        <!--</div>-->

<@page.footer/>