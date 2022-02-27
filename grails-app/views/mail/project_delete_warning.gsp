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
                            <h3>Hei.</h3>

                            <p class="lead">
                                Du mottar denne e-posten fordi du har et prosjekt som vil bli slettet snart (<%= toDeleteAt %>)
                            </p>

                            <p>
                                Klikk på <a href='<%= by %>/#/project/<%= projectId %>/information'> Hold deg i live-knappen</a> hvis du vil utsette den slettede datoen. <br />
                            </p>
                            <p>
                                Merk: Etter sletting er data mulig å hente tilbake fra backup fra kort periode. (Det påløper kostnader ved å hente data tilbake fra backup.) <br />
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
