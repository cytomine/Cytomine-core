<g:render template="/mail/header" model="[]"/>

<!-- BODY -->
<table class="body-wrap">
    <tr>
        <td></td>
        <td class="container" bgcolor="#FFFFFF">

            <div class="content">
                <table>
                    <tr>
                        <td>
                            <h3>Dear Madam/Sir,</h3>

                            <p class="lead">
                                You receive this email because the project <%= projectName %> will be deleted soon (<%= toDeleteAt %>)
                            </p>

                            <p>
                                Click <a href='<%= by %>/#/project/<%= projectId %>/information'> here</a> if you want to delay the deleted date. <br />
                            </p>

                            <!-- social & contact -->
                            <g:render template="/mail/social" model="[website :website, mailFrom: mailFrom, phoneNumber:phoneNumber]"/>

                        </td>
                    </tr>
                </table>
            </div><!-- /content -->

        </td>
        <td></td>
    </tr>
</table><!-- /BODY -->

<g:render template="/mail/footer" model="[by : by]"/>
