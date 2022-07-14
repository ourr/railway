package tm.salam.hazarLogistika.railway.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tm.salam.hazarLogistika.railway.helper.FileUploadUtil;
import tm.salam.hazarLogistika.railway.helper.ResponseTransfer;
import tm.salam.hazarLogistika.railway.models.Role;
import tm.salam.hazarLogistika.railway.models.User;
import tm.salam.hazarLogistika.railway.daos.RoleRepository;
import tm.salam.hazarLogistika.railway.daos.UserRepository;
import tm.salam.hazarLogistika.railway.dtos.UserDTO;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService{

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private  RoleRepository roleRepository;
    final String uploadDir = "src/main/resources/imageUsers/";

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public ResponseTransfer addNewLogist(final UserDTO userDTO, final MultipartFile image){

        if(userRepository.findUserByEmail(userDTO.getEmail())!=null){

            return new ResponseTransfer("email new logist already added",false);
        }
        List<Role> roles=new ArrayList<>();
        Role role=roleRepository.findRoleByName("ROLE_LOGIST");

        roles.add(role);

        User user=User.builder()
                .name(userDTO.getName())
                .surname(userDTO.getSurname())
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .roles(roles)
                .build();
        userRepository.save(user);
        User savedUser=userRepository.findUserByEmail(userDTO.getEmail());

        if(savedUser!=null){

            if(image!=null) {
                String extension="";
                for(int i=image.getOriginalFilename().length()-1; i>=0;i--){
                    extension=image.getOriginalFilename().charAt(i)+extension;
                    if(image.getOriginalFilename().charAt(i)=='.'){
                        break;
                    }
                }

                final String fileName = "image user " + String.valueOf(savedUser.getId())+extension;

                try {

                    FileUploadUtil.saveFile(uploadDir, fileName, image);
                    savedUser.setImagePath(uploadDir+fileName);

                } catch (IOException e) {
                    e.printStackTrace();

                    return new ResponseTransfer("logist successful added but image don't saved",true);
                }
            }

            return new ResponseTransfer("Logist successful added",true);
        }else{

            return new ResponseTransfer("Logist don't added",false);
        }
    }

    @Override
    @Transactional
    public ResponseTransfer removeLogistById(final int id) throws Exception {

        User logist=userRepository.findUserById(id);

        if(logist==null){

            return new ResponseTransfer("logist not found with this id",false);
        }else{

            for(Role role:logist.getRoles()){

                if(Objects.equals(role.getName(),"ROLE_ADMIN")){

                    throw new Exception("delete admin impossible");
                }
            }
            userRepository.deleteById(id);
        }
        String imagePath=logist.getImagePath();

        if(userRepository.findUserById(id)==null){

            File file=new File(imagePath);

            if(file.exists()){
                file.delete();
            }
            return new ResponseTransfer("logist successful removed",true);
        }else{

            return new ResponseTransfer("logist dont' removed",false);
        }
    }

    @Override
    public List<UserDTO> getAllUserDTO(){

        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private UserDTO toDTO(final User user) {

        final List<Role>roles=user.getRoles();
        List<String>nameRoles=new ArrayList<>();

        for(final Role role:roles){

            nameRoles.add(role.getName());
        }

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .imagePath(user.getImagePath())
                .roles(nameRoles)
                .build();
    }

    @Override
    public UserDTO getUserDTOById(final int id){

        User user=userRepository.findUserById(id);

        if(user==null){

            return null;
        }else{

            return toDTO(user);
        }
    }

    @Override
    public User getUserByEmail(final String email){

        User user=userRepository.findUserByEmail(email);

        return user;
    }

    @Override
    @Transactional
    public ResponseTransfer editProfile(final UserDTO userDTO, final int id, final MultipartFile image){

        User user=userRepository.findUserById(id);

        if(user==null){

            return new ResponseTransfer("user not found",false);
        }
        if(userDTO!=null) {
            if (userDTO.getName() != null && !userDTO.getName().isEmpty()) {
                user.setName(userDTO.getName());
            }
            if (userDTO.getSurname() != null && !userDTO.getSurname().isEmpty()) {
                user.setSurname(userDTO.getSurname());
            }
            if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty()) {
                user.setEmail(userDTO.getEmail());
            }
            if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            }
        }
        if(image!=null && !image.isEmpty()){

            String fileName;
            String extension="";
            if(user.getImagePath()!=null) {

                File file = new File(user.getImagePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            for(int i=image.getOriginalFilename().length()-1; i>=0;i--){
                extension=image.getOriginalFilename().charAt(i)+extension;
                if(image.getOriginalFilename().charAt(i)=='.'){
                    break;
                }
            }

            fileName="image user "+String.valueOf(user.getId())+extension;
            try {

                FileUploadUtil.saveFile(uploadDir,fileName,image);
                user.setImagePath(uploadDir+fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(userRepository.findUserByEmail(user.getEmail())!=null){

            return new ResponseTransfer("profile user successful edited ",true);
        }else{

            return new ResponseTransfer("profile user don't edited",false);
        }
    }

    @Override
    public UserDTO getUserDTOByEmail(final String email){

        User user=userRepository.findUserByEmail(email);

        if(user==null){

            return null;
        }else{

            return toDTO(user);
        }
    }

    @Override
    public List<UserDTO>getAllLogistDTO(){

        Role role=roleRepository.findRoleByName("ROLE_LOGIST");
        List<User>users=userRepository.findUsersByRoles(role);
        List<UserDTO>userDTOS=new ArrayList<>();

        users.forEach(user -> {
            userDTOS.add(toDTO(user));
        });

        return userDTOS;
    }

}
