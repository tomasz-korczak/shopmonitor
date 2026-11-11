package smon.model;

/**
 * What a value inside a list element is. The extractor uses the first four.
 * <p>
 * The NONE_* values are not used by the code but are <b>not dead</b>: the ordinals 0-7 are the class labels in the
 * last column of ml/test_data.csv (element classification experiment). Do not remove or reorder the constants.
 */
public enum ElementType {
    IMAGE,
    DESTINATION,
    PRICE,
    CONTENT,
    NONE_SCRIPT_META,     // <script>, <meta>, <link>
    NONE_NAVIGATION,      // header, menu, nav, social
    NONE_DECORATIVE,      // ozdoby, puste kontenery, separatory
    NONE_FOOTER           // stopka strony
}
