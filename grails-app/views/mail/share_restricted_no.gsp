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
                            <h3>Hei,</h3>

                            <p class="lead"><%= from %> delte en annotasjon med deg.</p>

                            <p class="callout">
                                Naviger til <a href='<%= shareAnnotationURL %>'><%= shareAnnotationURL %></a> for å besvare.<br/>
                                Naviger til <a href='<%= annotationURL %>'><%= annotationURL %></a> for å se på annotasjonen i kontekst eller klikk på bildet.
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