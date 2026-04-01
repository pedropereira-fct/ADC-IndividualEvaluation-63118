package pt.unl.fct.di.adc.indeval.util;

public class ModifyAccountAttributesData {

    public String username;
    public Attributes attributes;

    public ModifyAccountAttributesData () { }

    public ModifyAccountAttributesData(String username, Attributes attributes) {
        this.username = username;
        this.attributes = attributes;
    }

    public static class Attributes {
        public String phone;
        public String address;

        public boolean validModification() {
            return nonEmptyOrBlankField(phone) || nonEmptyOrBlankField(address);
        }

        private boolean nonEmptyOrBlankField(String field) {
            return field != null && !field.isBlank();
        }
    }

}
