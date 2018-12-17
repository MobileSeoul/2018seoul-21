
# 서울시앱공모전  Server-side 
## Start up
1. *yarn dev* or *npm run dev*
2. At First, sign up your account to the server.

> POST http://localhost:3001/api/users

     {
    	  "email": "test@test1.com",
    	  "password": "1234",
    	  "displayName": "wfr112",
    	  "phoneNumber": "010-0001-0000"
     }

3. Login.

>POST http://localhost:3001/api/auth/session

    {
          "email": "test@test1.com",
          "password": "1234"
    }
4. Check whether you were log in.
 >GET http://localhost:3001/api/auth/me

       {
          "message": {
              "_id": "5b8e4249f9f8184d0c825c6a",
              "authId": "local:test@test1.com",
              "email": "test@test1.com",
              "password": "qH1WrjR2VGgXOmwtJ0gW4vukJaxR2BjPXpGrso/5IOtPoHWcytiOyvlpcqXTt/Em7cgfrwkJ8HZMTjz2I+jVkY/yoFoMreU8D+q5if+3CziixwhoLi0kEcp9IKqb0ROsGz32GgnJpGctcXBX56G7J2aQlaM/R4eDoRi+hSf/Gws=",
              "salt": "y9AuW4/YK18vINhC4zvrGLp/osoog8wq8ojoJ/rSvUwhv4v0l572pL+SdsGSwVucacBxhnt73NPBsng24Hrq5g==",
              "displayName": "wfr112",
              "phoneNumber": "010-0001-0000",
              "date": "2018-09-04T08:28:57.461Z",
              "__v": 0
        },
           "success": true
    }

## Routers

**Auth**


----------


SignUp

>POST http://localhost:3001/api/users
> req:
> 
> { 	"email": "test@test1.com", 	"password": "1234", 	"displayName":
> "wfr112", 	"phoneNumber": "010-0001-0000" }
> res:
> 
>     {
>         "data": {
>             "_id": "5ba1faf6b81deb3f7cef3883",
>             "authId": "local:test@test1.com",
>             "email": "test@test1.com",
>             "password": "gaTQ8TE2pyjzfoN4bODTubGuUs3ZGS8EX6VmaDjecoprT69l+lYqTc1Lm5o81fjexjperiv/qEXHzhNChdZo93jpw1jIIPp87GBQutymkwhUnUtol2Mu3GOpU5CIVrz9slLNYtQQr0KRefBCyY4R8an9Jz0Y0gEjNMWD6DXTiys=",
>             "salt": "lS6SroxxhbGnEc4bi5S/5zmeAacArMVgNhtcddH/xSS8T8WKw5jqzoaKLMzUXPkzBkPkOGIIaZ56XLpcjSxJrw==",
>             "displayName": "wfr112",
>             "phoneNumber": "010-0001-0000",
>             "date": "2018-09-19T07:29:58.787Z",
>             "__v": 0
>         } }

Login

>POST http://localhost:3001/api/auth/session
> req:
> 
>     { 	"email": "test@test1.com",  "password": "1234" } res: { "data": {
>         "_id": "5ba1faf6b81deb3f7cef3883",
>         "authId": "local:test@test1.com",
>         "email": "test@test1.com",
>         "password": "gaTQ8TE2pyjzfoN4bODTubGuUs3ZGS8EX6VmaDjecoprT69l+lYqTc1Lm5o81fjexjperiv/qEXHzhNChdZo93jpw1jIIPp87GBQutymkwhUnUtol2Mu3GOpU5CIVrz9slLNYtQQr0KRefBCyY4R8an9Jz0Y0gEjNMWD6DXTiys=",
>         "salt": "lS6SroxxhbGnEc4bi5S/5zmeAacArMVgNhtcddH/xSS8T8WKw5jqzoaKLMzUXPkzBkPkOGIIaZ56XLpcjSxJrw==",
>         "displayName": "wfr112",
>         "phoneNumber": "010-0001-0000",
>         "date": "2018-09-19T07:29:58.787Z",
>         "__v": 0 } }

Logout

>GET http://localhost:3001/api/auth/session
>req: 
>res:  {}


Me

>GET http://localhost:3001/api/users/me
>req:
>res:
> 
>     {
>         "data": {
>             "_id": "5ba1faf6b81deb3f7cef3883",
>             "authId": "local:test@test1.com",
>             "email": "test@test1.com",
>             "password": "gaTQ8TE2pyjzfoN4bODTubGuUs3ZGS8EX6VmaDjecoprT69l+lYqTc1Lm5o81fjexjperiv/qEXHzhNChdZo93jpw1jIIPp87GBQutymkwhUnUtol2Mu3GOpU5CIVrz9slLNYtQQr0KRefBCyY4R8an9Jz0Y0gEjNMWD6DXTiys=",
>             "salt": "lS6SroxxhbGnEc4bi5S/5zmeAacArMVgNhtcddH/xSS8T8WKw5jqzoaKLMzUXPkzBkPkOGIIaZ56XLpcjSxJrw==",
>             "displayName": "wfr112",
>             "phoneNumber": "010-0001-0000",
>             "date": "2018-09-19T07:29:58.787Z",
>             "__v": 0
>         } }

//@desc : 페이스북 로그인
 //@router : GET http://localhost:3001/api/auth/facebook
 
**Memo**

----------
  //@desc : 주어진 tag가 속한 모든 메모를 출력함
  //@router : GET http://localhost:3001/api/memos/findByTag
  //@query : tag: String

  //@desc : 특정 유저에게 속한 모든 메모의 개수를 보여줌
  //@router : GET http://localhost:3001/api/memos/:id/count
  //@params : id: String
	
  //@desc : 자신에게 속한 모든 메모의 개수를 보여줌
  //@router : GET http://localhost:3001/api/memos/count

  //@desc : 주어진 좌표에서 가까운 메모 최대 30개 반환, loc img필드만 가져옴
  //@router : GET http://localhost:3001/api/memos/near
  //@query : lng: String, lat: String, skip: String, limit: String
  

     http://localhost:3001/api/memos/near?lng=127.036624&lat=37.612616

  
  //@desc : 특정 유저에 속해있는 모든 메모 출력
  //@router : GET http://localhost:3001/api/memos/:id
  //@params : id: String
  //@query : skip: String, limit: String

  //@desc : 자신에게 속해있는 모든 메모 출력
  //@router : GET http://localhost:3001/api/memos/:id
  //@query : skip: String, limit: String

  //@desc : 자신에게 메모를 추가함
  //@router : POST http://localhost:3001/api/memos
  //@body : text: String, img: Object, tags: String, loc: Object [log, lat]

    {
          "img": Object,
          "text": "남영동",
          "loc": {
          	"type": "Point",
            "coordinates": [126.972967, 37.541710] //주의! [log(경도), lat(위도)]
          },
          "tags": "#HanGang#River"	
    }

  //@desc : 자신의 모든 메모를 삭제함
  //@router : DELETE http://localhost:3001/api/memos

  //@desc : 자신의 특정한 메모 한 개를 삭제함
  //@router : DELETE http://localhost:3001/api/memos/:id
  //@params : id: String

  //@desc : 자신의 특정한 memo의 사진, 텍스트를 바꿈
  //@router : PUT http://localhost:3001/api/memos/:id/TextAndImage
  //@params : id: String
  //@body: img: Object, text: String


**Comment**

----------
//@desc : 특정 메모에 속해있는 이모찌와 유저를 전부 가져옴
//@router : POST http://localhost:3001/api/comments/:id
//@params : id: String
//@query : skip: String, limit: String
	
//@desc : 코멘트 추가
//@router : POST http://localhost:3001/api/comments
//@body : id: String, emoji: String, memo: String

//@desc : 특정 메모의 자신의 코멘트 삭제
//@router: DELETE http://localhost:3001/api/comments/:id
//@params : id: String

//@desc : 특정 메모의 자신의 특정 코멘트 수정
//@router: PUT http://localhost:3001/api/comments/:id
//@params : id: String
//@body : emoji: String
**User**

----------
//@desc : 로컬 회원가입
//@router: DELETE http://localhost:3001/api/users
//@body : email: String, password: String, displayName: String, img: Object

//@desc : 자신의 유저 정보 가져오기
//@router : GET http://localhost:3001/api/users/me

//@desc : 특정 유저 정보 가져오기
//@router : GET http://localhost:3001/api/users/:id
//@params : id: String

//@desc : 자신의 계정을 삭제함
//@router : DELETE http://localhost:3001/api/users

//@desc : 자신의 계정을 수정
//@router : PUT http://localhost:3001/api/users
//@body : email: String, password: String, displayName: String, img: Object

//@desc : 자신의 프로필 사진을 수정
//@router : PUT http://localhost:3001/api/users/profile
//@body : img: Object

//@desc : 자신의 프로필 사진 삭제
//@router : DELETE http://localhost:3001/api/users/profile