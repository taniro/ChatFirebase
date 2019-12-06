package tads.eaj.ufrn.chatfirebase

/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
Modified by Taniro 15/11/2019 to add support to kotlin
 */
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.auth.AuthUI
import java.util.*
import kotlin.collections.ArrayList
import android.widget.Toast
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.OnSuccessListener


class MainActivity : AppCompatActivity() {

    private val CODIGO_FOTO = 57
    private val CODIGO_LOGAR = 55
    private val ANONYMOUS = "anonymous"
    private val DEFAULT_MSG_LENGTH_LIMIT = 1000

    private var friendlyMessages = ArrayList<FriendlyMessage>()
    private val mMessageAdapter by lazy {
        MessageAdapter(this, R.layout.item_message, friendlyMessages)
    }

    private lateinit var mUsername: String

    /*
    * Acesso ao banco
     */
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mMessagesDatabaseReference: DatabaseReference

    /*
    * Listener de dados do banco
     */
    private var mChildEventListener: ChildEventListener? = null

    /*
    * Instância do Firebase Auth, para o login e do seu respectivo listener.
    */
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mAuthStateListener: FirebaseAuth.AuthStateListener

    /*
    * Instância do Firebase storage
     */
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mStorageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsername = ANONYMOUS

        /*
         INICIO FIREBASE
         */

        /*
        * Instância do firebase utilizada para buscar a referência ao firebasedatabase
         */

        mFirebaseDatabase = FirebaseDatabase.getInstance()
        /*
        * Referencia para uma "familia de dados" na firebase database
        */
        mMessagesDatabaseReference = mFirebaseDatabase.reference.child("messages")

        /*
        Inicialização do Auth, apos a inicialização do banco
        */
        mFirebaseAuth = FirebaseAuth.getInstance();

        /*
        * Inicializando o Storage e selecionando uma referencia.
        */
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageReference = mFirebaseStorage.reference.child("chat_photos");


        /*
        FIM FIREBASE
         */

        messageListView.adapter = mMessageAdapter

        // ImagePickerButton shows an image picker to upload a image for a message
        photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                CODIGO_FOTO
            )
        }

        messageEditText.filters = arrayOf(InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT))
        messageEditText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(texto: CharSequence?, p1: Int, p2: Int, p3: Int) {
                sendButton.isEnabled = texto.toString().trim().isNotEmpty()
            }

        })

        sendButton.setOnClickListener {
            //Criado um objeto que será enviado para o Firebase
            val friendlyMessage = FriendlyMessage(messageEditText.text.toString(), mUsername, null)

            //O método push é usado para informar que essa é uma nova row, logo um ID será gerado.
            mMessagesDatabaseReference.push().setValue(friendlyMessage)
            Log.i("INFO", "enviado?")

            // Clear input box
            messageEditText.setText("")
        }

        /*
        LEITURA DOS DADOS - TEMPORARIO
         */

        /*
        mChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                //metodo chamado quando uma nova mensagem é inserida na lista de mensagens.


                // * DataSnapshot contem exatamente o que mudou na referencia do banco de dados que queremos ler
                // * Essa mensagem será deserializada ao apontar como parametro a classe FriendlyMessage que contem
                // * exatamente os mesmos atributos da mensagem que estã sendo recebida.
                // *
                // * dataSnapshot.getValue(FriendlyMessage.class)

                val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
                mMessageAdapter.add(friendlyMessage)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                //metodo chamado quando uma o conteúdo de uma mensagem é alterado.
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                //metodo chamado quando uma mensagem é removida da lista de mensagens.
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
                //método chamado de uma mensagem mudou de posição na lista

            }

            override fun onCancelled(databaseError: DatabaseError) {
                //chamado quando algum erro ocorreu...geralmente é chamado se você não possui permissão para acessar os dados

            }
        }

        mMessagesDatabaseReference.addChildEventListener(mChildEventListener)

         */

        /*
        FIM LEITURA DOS DADOS
         */

        mAuthStateListener =
            FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    //logado
                    //Toast.makeText(MainActivity.this, "Logado", Toast.LENGTH_SHORT).show();
                    onSignInInitialize(user.displayName)
                } else {
                    //não-logado
                    onSignOutCleanUp()

                    //chama o fluxo de login
                    startActivityForResult(
                        AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(
                                Arrays.asList(
                                    AuthUI.IdpConfig.GoogleBuilder().build(),
                                    AuthUI.IdpConfig.EmailBuilder().build()
                                )
                            )
                            .build(),
                        CODIGO_LOGAR
                    )
                }
            }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                //logout
                AuthUI.getInstance().signOut(this)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth.addAuthStateListener(mAuthStateListener)
    }

    override fun onPause() {
        super.onPause()
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener)
        detachDatabaseReadListener();
        mMessageAdapter.clear()
    }

    private fun onSignInInitialize(username: String?) {
        mUsername = username?:ANONYMOUS
        attachDatabaseReadListener()

    }

    private fun onSignOutCleanUp() {
        mUsername = ANONYMOUS
        mMessageAdapter.clear()
        detachDatabaseReadListener()
    }


    private fun attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
                    mMessageAdapter.add(friendlyMessage)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            }
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener!!)
        }
    }
    private fun detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener!!)
            mChildEventListener = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_LOGAR) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bem-vindo", Toast.LENGTH_SHORT).show()
            } else if (resultCode == RESULT_CANCELED) {
                finish()
            }
        } else if (requestCode == CODIGO_FOTO && resultCode == RESULT_OK) {
            val selectedImageUri = data!!.data

            val photoref =  mStorageReference.child(mUsername + "_" + selectedImageUri!!.lastPathSegment)
            //upload da imagem

            //addOnSuccessListener para saber quando a imagem foi enviada, para então adicionar como um objeto de Messages.
            photoref.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    // Get a URL to the uploaded content
                    taskSnapshot.storage.getDownloadUrl()
                        .addOnSuccessListener(OnSuccessListener<Any> { uri ->
                            //Log.i("TESTE", uri.toString());
                            val friendlyMessage = FriendlyMessage(null, mUsername, uri.toString())
                            mMessagesDatabaseReference.push().setValue(friendlyMessage)
                        })
                }
        }
    }
}
