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
                            <h3>Til prosjekteier</h3>

                            <p class="lead">
                                Du mottar denne e-post da du er registert som eier av et prosjekt i <%= hv_instance %> som slettes (<%= toDeleteAt %>)
                            </p>

                            <p>
                                Trykk på <a href='<%= by %>/#/project/<%= projectId %>/information'> utsett sletting</a> hvis du vil forlenge prosjektets levetid. <br />
                            </p>
                            <p>
                                Merk: Når data slettes fra Cytomine er det ikke mulig gjenskape disse data. <br />
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
