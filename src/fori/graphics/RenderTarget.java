package fori.graphics;

import java.util.ArrayList;

public class RenderTarget extends Disposable {

    private ArrayList<RenderTargetAttachment> attachments;




    public RenderTarget(Disposable parent) {
        super(parent);
        attachments = new ArrayList<>();
    }

    public void addAttachment(RenderTargetAttachment attachment) {
        attachments.add(attachment);
    }

    public RenderTargetAttachment getAttachment(long mask) {
        for(RenderTargetAttachment attachment : attachments) {
            if((attachment.getFlags() & mask) != 0) return attachment;
        }

        return null;
    }



    @Override
    public void dispose() {

    }
}
