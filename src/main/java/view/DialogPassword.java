package view;

import java.awt.GraphicsEnvironment;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;

/** Input password khusus host IDE; seluruh komponen tetap pada layer view. */
final class DialogPassword {
    private DialogPassword() { }
    static JPasswordField field() {
        JPasswordField field=new JPasswordField(24);
        field.setEchoChar('*');
        return field;
    }
    static char[] baca(String prompt) {
        if(GraphicsEnvironment.isHeadless()) throw new IllegalArgumentException("Dialog password memerlukan desktop. Jalankan di terminal atau nonaktifkan APP_PASSWORD_DIALOG.");
        char[][] hasil=new char[1][];
        Runnable tampil=()->{
            JPasswordField field=field();
            JOptionPane pane=new JOptionPane(new Object[]{prompt+":",field},JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog=pane.createDialog(null,"Password aplikasi");
            // IDE/Output dapat berada di proses berbeda dan menutupi dialog tanpa owner.
            dialog.setAlwaysOnTop(true);
            dialog.setLocationRelativeTo(null);
            dialog.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent e) {
                    dialog.toFront();
                    dialog.requestFocus();
                    SwingUtilities.invokeLater(field::requestFocusInWindow);
                }
            });
            field.addActionListener(e->pane.setValue(JOptionPane.OK_OPTION));
            try {
                dialog.setVisible(true);
                if(Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())) hasil[0]=field.getPassword();
            } finally { field.setText(""); dialog.dispose(); }
        };
        try {
            if(SwingUtilities.isEventDispatchThread()) tampil.run(); else SwingUtilities.invokeAndWait(tampil);
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new InputBerakhir(true); }
        catch(InvocationTargetException e) { throw new IllegalArgumentException("Dialog password tidak dapat dibuka.",e.getCause()); }
        if(hasil[0]==null) throw new InputBerakhir(false);
        return hasil[0];
    }
}
