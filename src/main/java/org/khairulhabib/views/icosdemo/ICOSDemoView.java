package org.khairulhabib.views.icosdemo;

import org.apache.commons.io.IOUtils;
import org.khairulhabib.data.entity.Person;
import org.khairulhabib.data.service.IcosConfig;
import org.khairulhabib.data.service.PersonService;
import org.khairulhabib.data.service.UploadService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.khairulhabib.views.main.MainView;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;

@Route(value = "upload", layout = MainView.class)
@PageTitle("ICOS Demo")
@CssImport("./styles/views/icosdemo/i-cos-demo-view.css")
@RouteAlias(value = "", layout = MainView.class)
public class ICOSDemoView extends Div {

    private TextField accessKey = new TextField("Access Key");
    private TextField secretKey = new TextField("Secret Key");
    private TextField bucket = new TextField("Bucket");
    private Select<String> region = new Select<>();

    private String filename = "";

    @Autowired
    UploadService uploadService;

    private MemoryBuffer buffer = new MemoryBuffer();
    private Upload upload = new Upload(buffer);
    Div output = new Div();

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Upload");

    public ICOSDemoView(PersonService personService) {
        setId("i-cos-demo-view");

        add(createTitle());
        add(createFormLayout());
        add(createButtonLayout());
        clearForm();

        cancel.addClickListener(e -> clearForm());
        save.addClickListener(e -> {
            // call upload service here
            IcosConfig config = new IcosConfig();
            config.setAccessKey(accessKey.getValue());
            config.setSecretKey(secretKey.getValue());
            config.setRegion(region.getValue());
            config.setObjectKey(filename);
            config.setBucket(bucket.getValue());
            uploadService.upload(config, buffer);
            Notification.show("File uploaded successfully.");
            clearForm();
        });

        upload.setMaxFileSize(52428800);

        upload.addSucceededListener(event -> {
            Component component = createComponent(event.getMIMEType(),
                    event.getFileName(), buffer.getInputStream());
            filename = event.getFileName();
            showOutput(event.getFileName(), component, output);
        });
    }

    private void clearForm() {
        // do nothing
    }

    private Component createComponent(String mimeType, String fileName,
        InputStream stream) {
        if (mimeType.startsWith("text")) {
            return createTextComponent(stream);
        } else if (mimeType.startsWith("image")) {
            Image image = new Image();
            try {

                byte[] bytes = IOUtils.toByteArray(stream);
                image.getElement().setAttribute("src", new StreamResource(
                        fileName, () -> new ByteArrayInputStream(bytes)));
                try (ImageInputStream in = ImageIO.createImageInputStream(
                        new ByteArrayInputStream(bytes))) {
                    final Iterator<ImageReader> readers = ImageIO
                            .getImageReaders(in);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            reader.setInput(in);
                            image.setWidth(reader.getWidth(0) + "px");
                            image.setHeight(reader.getHeight(0) + "px");
                        } finally {
                            reader.dispose();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return image;
        }
        Div content = new Div();
        String text = String.format("Mime type: '%s'\nSHA-256 hash: '%s'",
                mimeType, MessageDigestUtil.sha256(stream.toString()));
        content.setText(text);
        return content;

    }

    private Component createTextComponent(InputStream stream) {
        String text;
        try {
            text = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            text = "exception reading stream";
        }
        return new Text(text);
    }

    private void showOutput(String text, Component content,
            HasComponents outputContainer) {
        HtmlComponent p = new HtmlComponent(Tag.P);
        p.getElement().setText(text);
        outputContainer.add(p);
        outputContainer.add(content);
    }

    private Component createTitle() {
        return new H3("Cloud Object Storage Credential");
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        region.setItems("jp-tok","jp-osa","au-syd","eu-de","eu-gb","us-east","us-south");
        region.setPlaceholder("select region");
        region.setLabel("Region");
        formLayout.add(accessKey, secretKey, bucket,region,upload);
        return formLayout;
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addClassName("button-layout");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save);
        buttonLayout.add(cancel);
        return buttonLayout;
    }

}
