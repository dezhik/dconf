<#import "page.ftl" as page>
<@page.header/>

        <!--<#include "page.ftl">-->


Last update: ${(lastUpdate?string('d MMM HH:mm:ss'))!'--'}

<!--    <div class="input-group mt-3" style="width:350px">-->
<!--        <input type="text" class="form-control" placeholder="Search by property name" aria-label="Property name" aria-describedby="button-property">-->
<!--        <button class="btn btn-outline-secondary" type="button" id="button-property">Search</button>-->
<!--    </div>-->


<@page.decoratedList properties=properties/>

<div class="container">

</div>

<@page.footer/>