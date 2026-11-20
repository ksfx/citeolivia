package com.citeolivia.web;

import org.springframework.web.util.HtmlUtils;

/**
 * Office add-in manifest (XML, add-in only manifest) for Word on the web, Windows and Mac. Adds a
 * "CiteOlivia" button to the Home tab that opens the task pane {@code /word/taskpane}.
 */
final class WordManifest {

    /** Stable add-in id. Never change it, otherwise Word treats the add-in as a different one. */
    static final String ADDIN_ID = "0efb4387-8ce7-4ccb-943f-e36690f8dd07";

    static final String VERSION = "1.0.0.0";

    private WordManifest() {}

    static String xml(String publicUrl) {
        String base = HtmlUtils.htmlEscape(publicUrl.replaceAll("/+$", ""));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <OfficeApp xmlns="http://schemas.microsoft.com/office/appforoffice/1.1"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xmlns:bt="http://schemas.microsoft.com/office/officeappbasictypes/1.0"
                           xmlns:ov="http://schemas.microsoft.com/office/taskpaneappversionoverrides"
                           xsi:type="TaskPaneApp">
                  <Id>%1$s</Id>
                  <Version>%2$s</Version>
                  <ProviderName>CiteOlivia</ProviderName>
                  <DefaultLocale>de-DE</DefaultLocale>
                  <DisplayName DefaultValue="CiteOlivia"/>
                  <Description DefaultValue="Zitate und Literaturverzeichnis aus deinen CiteOlivia-Projekten in Word einfügen."/>
                  <IconUrl DefaultValue="%3$s/word/icon-32.png"/>
                  <HighResolutionIconUrl DefaultValue="%3$s/word/icon-64.png"/>
                  <SupportUrl DefaultValue="%3$s/"/>
                  <AppDomains>
                    <AppDomain>%3$s</AppDomain>
                  </AppDomains>
                  <Hosts>
                    <Host Name="Document"/>
                  </Hosts>
                  <Requirements>
                    <Sets DefaultMinVersion="1.3">
                      <Set Name="WordApi" MinVersion="1.3"/>
                    </Sets>
                  </Requirements>
                  <DefaultSettings>
                    <SourceLocation DefaultValue="%3$s/word/taskpane"/>
                  </DefaultSettings>
                  <Permissions>ReadWriteDocument</Permissions>
                  <VersionOverrides xmlns="http://schemas.microsoft.com/office/taskpaneappversionoverrides" xsi:type="VersionOverridesV1_0">
                    <Hosts>
                      <Host xsi:type="Document">
                        <DesktopFormFactor>
                          <GetStarted>
                            <Title resid="CO.GetStarted.Title"/>
                            <Description resid="CO.GetStarted.Description"/>
                            <LearnMoreUrl resid="CO.Home.Url"/>
                          </GetStarted>
                          <ExtensionPoint xsi:type="PrimaryCommandSurface">
                            <OfficeTab id="TabHome">
                              <Group id="CO.Group">
                                <Label resid="CO.Group.Label"/>
                                <Icon>
                                  <bt:Image size="16" resid="CO.Icon.16"/>
                                  <bt:Image size="32" resid="CO.Icon.32"/>
                                  <bt:Image size="80" resid="CO.Icon.80"/>
                                </Icon>
                                <Control xsi:type="Button" id="CO.TaskpaneButton">
                                  <Label resid="CO.Button.Label"/>
                                  <Supertip>
                                    <Title resid="CO.Button.Label"/>
                                    <Description resid="CO.Button.Tooltip"/>
                                  </Supertip>
                                  <Icon>
                                    <bt:Image size="16" resid="CO.Icon.16"/>
                                    <bt:Image size="32" resid="CO.Icon.32"/>
                                    <bt:Image size="80" resid="CO.Icon.80"/>
                                  </Icon>
                                  <Action xsi:type="ShowTaskpane">
                                    <TaskpaneId>CiteOlivia</TaskpaneId>
                                    <SourceLocation resid="CO.Taskpane.Url"/>
                                  </Action>
                                </Control>
                              </Group>
                            </OfficeTab>
                          </ExtensionPoint>
                        </DesktopFormFactor>
                      </Host>
                    </Hosts>
                    <Resources>
                      <bt:Images>
                        <bt:Image id="CO.Icon.16" DefaultValue="%3$s/word/icon-16.png"/>
                        <bt:Image id="CO.Icon.32" DefaultValue="%3$s/word/icon-32.png"/>
                        <bt:Image id="CO.Icon.80" DefaultValue="%3$s/word/icon-80.png"/>
                      </bt:Images>
                      <bt:Urls>
                        <bt:Url id="CO.Taskpane.Url" DefaultValue="%3$s/word/taskpane"/>
                        <bt:Url id="CO.Home.Url" DefaultValue="%3$s/"/>
                      </bt:Urls>
                      <bt:ShortStrings>
                        <bt:String id="CO.GetStarted.Title" DefaultValue="CiteOlivia ist bereit"/>
                        <bt:String id="CO.Group.Label" DefaultValue="CiteOlivia"/>
                        <bt:String id="CO.Button.Label" DefaultValue="CiteOlivia"/>
                      </bt:ShortStrings>
                      <bt:LongStrings>
                        <bt:String id="CO.GetStarted.Description" DefaultValue="Öffne CiteOlivia über die Schaltfläche auf der Registerkarte Start."/>
                        <bt:String id="CO.Button.Tooltip" DefaultValue="Zitate und Literaturverzeichnis aus deinem CiteOlivia-Projekt einfügen."/>
                      </bt:LongStrings>
                    </Resources>
                  </VersionOverrides>
                </OfficeApp>
                """.formatted(ADDIN_ID, VERSION, base);
    }
}
