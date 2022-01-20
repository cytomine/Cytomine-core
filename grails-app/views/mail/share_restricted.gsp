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
                            <h3>Hi,</h3>

                            <p class="lead"><%= from %> shared an annotation with you.</p>

                            <p class="callout">
                                Navigate to <a href='<%= shareAnnotationURL %>'><%= shareAnnotationURL %></a> in order to reply.<br/>
                                Navigate to <a href='<%= annotationURL %>'><%= annotationURL %></a> in order to view the annotation within its
                            context, or click on the thumbnail.
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