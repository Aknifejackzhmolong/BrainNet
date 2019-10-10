package com.brainsci.form;

public class BsciProcessor {
    private String id;
    private String boot = null;
    private String[] params = null;
    private String[] replaceAll = null;
    private String zipTaget = null;
    private String deleteTaget = null;
    private String outputTaget = null;
    private boolean enableMail = false;
    private boolean autoDestroy = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBoot() {
        return boot;
    }

    public void setBoot(String boot) {
        this.boot = boot;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public String[] getReplaceAll() {
        return replaceAll;
    }

    public void setReplaceAll(String[] replaceAll) {
        this.replaceAll = replaceAll;
    }

    public String getZipTaget() {
        return zipTaget;
    }

    public void setZipTaget(String zipTaget) {
        this.zipTaget = zipTaget;
    }

    public String getDeleteTaget() {
        return deleteTaget;
    }

    public void setDeleteTaget(String deleteTaget) {
        this.deleteTaget = deleteTaget;
    }

    public String getOutputTaget() {
        return outputTaget;
    }

    public void setOutputTaget(String outputTaget) {
        this.outputTaget = outputTaget;
    }

    public boolean isEnableMail() {
        return enableMail;
    }

    public void setEnableMail(boolean enableMail) {
        this.enableMail = enableMail;
    }

    public boolean isAutoDestroy() {
        return autoDestroy;
    }

    public void setAutoDestroy(boolean autoDestroy) {
        this.autoDestroy = autoDestroy;
    }
}
